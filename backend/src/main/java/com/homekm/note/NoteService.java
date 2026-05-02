package com.homekm.note;

import com.homekm.audit.AuditService;
import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.common.ChildAccountWriteException;
import com.homekm.common.ChildSafeService;
import com.homekm.common.EntityNotFoundException;
import com.homekm.common.EventBus;
import com.homekm.common.PageResponse;
import com.homekm.folder.Folder;
import com.homekm.folder.FolderRepository;
import com.homekm.note.dto.*;
import com.homekm.reminder.ReminderRepository;
import com.homekm.common.RequestContextHelper;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class NoteService {

    /** Cap on revisions kept per note — older entries are trimmed on every update. */
    private static final int REVISION_KEEP = 50;

    private final NoteRepository noteRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ReminderRepository reminderRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final ChildSafeService childSafeService;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final NoteRevisionRepository revisionRepository;
    private final com.homekm.common.ContentModerationService moderation;
    private final com.homekm.search.EmbeddingIndexer embeddingIndexer;

    public NoteService(NoteRepository noteRepository, ChecklistItemRepository checklistItemRepository,
                       ReminderRepository reminderRepository, FolderRepository folderRepository,
                       UserRepository userRepository, ChildSafeService childSafeService,
                       AuditService auditService, EventBus eventBus,
                       NoteRevisionRepository revisionRepository,
                       com.homekm.common.ContentModerationService moderation,
                       com.homekm.search.EmbeddingIndexer embeddingIndexer) {
        this.noteRepository = noteRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.reminderRepository = reminderRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.childSafeService = childSafeService;
        this.auditService = auditService;
        this.eventBus = eventBus;
        this.revisionRepository = revisionRepository;
        this.moderation = moderation;
        this.embeddingIndexer = embeddingIndexer;
    }

    public PageResponse<NoteSummary> list(Long folderId, int page, int size, UserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Note> notes;
        if (principal.isChild()) {
            notes = folderId != null
                    ? noteRepository.listByFolderChildSafe(folderId, pageable)
                    : noteRepository.listRootChildSafe(pageable);
        } else {
            notes = folderId != null
                    ? noteRepository.listByFolder(folderId, pageable)
                    : noteRepository.listRoot(pageable);
        }
        return PageResponse.of(notes.map(n -> NoteSummary.from(n,
                checklistItemRepository.countByNoteId(n.getId()),
                checklistItemRepository.countByNoteIdAndCheckedTrue(n.getId()))));
    }

    public NoteDetail getById(Long id, UserPrincipal principal) {
        Note note = findVisibleNote(id, principal);
        List<ChecklistItem> items = checklistItemRepository.findByNoteIdOrderBySortOrder(id);
        List<com.homekm.reminder.Reminder> reminders = reminderRepository.findByNoteId(id);
        return NoteDetail.from(note, items, reminders);
    }

    @Transactional
    public NoteDetail create(NoteRequest req, UserPrincipal principal) {
        User owner = userRepository.getReferenceById(principal.getId());
        Note note = new Note();
        note.setTitle(req.title());
        note.setBody(req.body());
        note.setLabel(req.label() != null ? req.label() : "custom");
        note.setOwner(owner);
        // Children cannot create templates.
        if (req.isTemplate() != null && req.isTemplate() && !principal.isChild()) {
            note.setTemplate(true);
        }

        if (req.folderId() != null) {
            Folder folder = folderRepository.findActiveById(req.folderId())
                    .orElseThrow(() -> new EntityNotFoundException("Folder", req.folderId()));
            note.setFolder(folder);
        }

        // Child: force child-safe; Adults: use provided value, but if absent
        // ask the moderation service for a recommendation. AUTO_SAFE/AUTO_ADULT
        // also stamps child_safe_review_at so the parental review queue stays
        // clear for items the heuristic was confident about; NEEDS_REVIEW
        // leaves the timestamp null so the queue surfaces them.
        if (principal.isChild()) {
            note.setChildSafe(true);
        } else if (req.isChildSafe() != null) {
            note.setChildSafe(req.isChildSafe());
            note.setChildSafeReviewAt(moderation.reviewedAt());
        } else {
            var verdict = moderation.classifyNote(req.title(), req.body());
            switch (verdict.verdict()) {
                case AUTO_SAFE -> {
                    note.setChildSafe(true);
                    note.setChildSafeReviewAt(moderation.reviewedAt());
                }
                case AUTO_ADULT -> {
                    note.setChildSafe(false);
                    note.setChildSafeReviewAt(moderation.reviewedAt());
                }
                case NEEDS_REVIEW -> {
                    note.setChildSafe(false);
                    // childSafeReviewAt left null -> appears in the queue.
                }
            }
        }

        noteRepository.save(note);

        // If unsafe note added to safe folder, demote the folder
        if (!note.isChildSafe()) {
            childSafeService.demoteFolderIfNeeded(req.folderId(), false);
        }

        embeddingIndexer.indexNote(note.getId(), note.getTitle(), note.getBody());
        return NoteDetail.from(note, List.of(), List.of());
    }

    public record RevisionResponse(long id, long noteId, String title, String body,
                                    String label, long editedBy, java.time.Instant editedAt) {
        public static RevisionResponse from(NoteRevision r) {
            return new RevisionResponse(r.getId(), r.getNote().getId(),
                    r.getTitle(), r.getBody(), r.getLabel(),
                    r.getEditedBy().getId(), r.getEditedAt());
        }
    }

    /** Newest-first list of stored revisions for a note. */
    public List<RevisionResponse> listRevisions(Long noteId, UserPrincipal principal) {
        Note note = findVisibleNote(noteId, principal);
        return revisionRepository.findByNoteIdOrderByEditedAtDesc(note.getId()).stream()
                .map(RevisionResponse::from)
                .toList();
    }

    /**
     * Restore the note to a previous revision. The note's current title /
     * body / label are themselves snapshotted into a new revision row first,
     * so the restore is itself reversible. Adults only.
     */
    @Transactional
    public NoteDetail restoreRevision(Long noteId, Long revisionId, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Note note = noteRepository.findActiveById(noteId)
                .orElseThrow(() -> new EntityNotFoundException("Note", noteId));
        NoteRevision target = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new EntityNotFoundException("Revision", revisionId));
        if (!target.getNote().getId().equals(noteId)) {
            throw new EntityNotFoundException("Revision", revisionId);
        }

        NoteRevision snapshot = new NoteRevision();
        snapshot.setNote(note);
        snapshot.setTitle(note.getTitle());
        snapshot.setBody(note.getBody());
        snapshot.setLabel(note.getLabel());
        snapshot.setEditedBy(userRepository.getReferenceById(principal.getId()));
        revisionRepository.save(snapshot);
        revisionRepository.trimToLast(note.getId(), REVISION_KEEP);

        note.setTitle(target.getTitle());
        note.setBody(target.getBody());
        note.setLabel(target.getLabel());
        noteRepository.save(note);

        List<ChecklistItem> items = checklistItemRepository.findByNoteIdOrderBySortOrder(noteId);
        List<com.homekm.reminder.Reminder> reminders = reminderRepository.findByNoteId(noteId);
        return NoteDetail.from(note, items, reminders);
    }

    public List<NoteSummary> listTemplates(UserPrincipal principal) {
        if (principal.isChild()) return List.of();
        return noteRepository.findAllTemplates().stream()
                .map(n -> NoteSummary.from(n,
                        checklistItemRepository.countByNoteId(n.getId()),
                        checklistItemRepository.countByNoteIdAndCheckedTrue(n.getId())))
                .toList();
    }

    /**
     * Clone a template into a fresh non-template note owned by the caller.
     * Body, label, child-safe flag, folder, and checklist items copy across;
     * reminders, tags, and pin state do not (templates are blueprints, not
     * snapshots). Adults only.
     */
    @Transactional
    public NoteDetail createFromTemplate(Long templateId, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Note tpl = noteRepository.findActiveById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template", templateId));
        if (!tpl.isTemplate()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "NOT_A_TEMPLATE");
        }
        User owner = userRepository.getReferenceById(principal.getId());
        Note copy = new Note();
        copy.setTitle(tpl.getTitle());
        copy.setBody(tpl.getBody());
        copy.setLabel(tpl.getLabel());
        copy.setOwner(owner);
        copy.setFolder(tpl.getFolder());
        copy.setChildSafe(tpl.isChildSafe());
        // copy.template remains false — this is a working copy.
        noteRepository.save(copy);

        // Clone checklist items, preserving order; reset checked state so the
        // new copy starts fresh.
        List<ChecklistItem> tplItems = checklistItemRepository.findByNoteIdOrderBySortOrder(templateId);
        for (ChecklistItem src : tplItems) {
            ChecklistItem dst = new ChecklistItem();
            dst.setNote(copy);
            dst.setText(src.getText());
            dst.setSortOrder(src.getSortOrder());
            dst.setChecked(false);
            checklistItemRepository.save(dst);
        }

        List<ChecklistItem> items = checklistItemRepository.findByNoteIdOrderBySortOrder(copy.getId());
        return NoteDetail.from(copy, items, List.of());
    }

    @Transactional
    public NoteDetail update(Long id, NoteRequest req, UserPrincipal principal) {
        Note note = noteRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note", id));

        // Optimistic-concurrency check before any mutation. Hibernate's
        // @Version would also catch this on flush via OptimisticLockException,
        // but checking here lets us throw a clean 409 with the live version
        // so the editor can re-load and surface a merge UI.
        if (req.expectedVersion() != null && req.expectedVersion() != note.getVersion()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "STALE_VERSION");
        }

        // Children can only edit their own notes
        if (principal.isChild()) {
            if (note.getOwner().getId() != principal.getId()) {
                throw new ChildAccountWriteException();
            }
            // Children cannot change child-safe flag
        }

        // Snapshot pre-edit state into note_revisions before applying changes
        // so the History tab can roll back. Skip when nothing changed about
        // title/body/label — moves and child-safe toggles aren't worth a row.
        boolean contentChanged =
                (req.title() != null && !req.title().equals(note.getTitle()))
             || (req.body() != null && !java.util.Objects.equals(req.body(), note.getBody()))
             || (req.label() != null && !req.label().equals(note.getLabel()));
        if (contentChanged) {
            NoteRevision rev = new NoteRevision();
            rev.setNote(note);
            rev.setTitle(note.getTitle());
            rev.setBody(note.getBody());
            rev.setLabel(note.getLabel());
            rev.setEditedBy(userRepository.getReferenceById(principal.getId()));
            revisionRepository.save(rev);
            revisionRepository.trimToLast(note.getId(), REVISION_KEEP);
        }

        if (req.title() != null) note.setTitle(req.title());
        if (req.body() != null) note.setBody(req.body());
        if (req.label() != null) note.setLabel(req.label());
        if (req.isTemplate() != null && !principal.isChild()) note.setTemplate(req.isTemplate());

        // Only adults can toggle child-safe
        if (!principal.isChild() && req.isChildSafe() != null) {
            note.setChildSafe(req.isChildSafe());
        }

        // Handle folder move
        if (req.folderId() != null) {
            Long currentFolderId = note.getFolder() != null ? note.getFolder().getId() : null;
            if (!req.folderId().equals(currentFolderId)) {
                Folder dest = folderRepository.findActiveById(req.folderId())
                        .orElseThrow(() -> new EntityNotFoundException("Folder", req.folderId()));
                note.setFolder(dest);
                // Destination folder is safe → note becomes safe
                boolean newSafe = childSafeService.resolveChildSafeOnMove(note.isChildSafe(), req.folderId());
                note.setChildSafe(newSafe);
                if (!note.isChildSafe()) {
                    childSafeService.demoteFolderIfNeeded(req.folderId(), false);
                }
            }
        }

        noteRepository.save(note);
        if (contentChanged) {
            embeddingIndexer.indexNote(note.getId(), note.getTitle(), note.getBody());
        }
        List<ChecklistItem> items = checklistItemRepository.findByNoteIdOrderBySortOrder(id);
        List<com.homekm.reminder.Reminder> reminders = reminderRepository.findByNoteId(id);
        eventBus.publish(EventBus.Event.broadcast("ItemUpdated",
                Map.of("type", "note", "id", id)));
        return NoteDetail.from(note, items, reminders);
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Note note = noteRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note", id));
        note.setDeletedAt(Instant.now());
        noteRepository.save(note);
        auditService.record(principal.getId(), "NOTE_DELETE", "note", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
    }

    @Transactional
    public void restore(Long id, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note", id));
        if (note.getDeletedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NOT_DELETED");
        }
        note.setDeletedAt(null);
        noteRepository.save(note);
        auditService.record(principal.getId(), "NOTE_RESTORE", "note", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
    }

    @Transactional
    public NoteDetail pin(Long id, UserPrincipal principal) {
        return updatePinState(id, Instant.now(), principal);
    }

    @Transactional
    public NoteDetail unpin(Long id, UserPrincipal principal) {
        return updatePinState(id, null, principal);
    }

    private NoteDetail updatePinState(Long id, Instant timestamp, UserPrincipal principal) {
        // findVisibleNote first (404 for child + adult-only) so children can't probe
        // hidden notes via 403 vs 404. Then ownership check (403 for not-yours).
        Note note = findVisibleNote(id, principal);
        if (principal.isChild() && note.getOwner().getId() != principal.getId()) {
            throw new ChildAccountWriteException();
        }
        boolean isPinned = note.getPinnedAt() != null;
        boolean shouldBePinned = timestamp != null;
        if (isPinned != shouldBePinned) {
            noteRepository.setPinnedAt(id, timestamp);
        }
        return getById(id, principal);
    }

    // --- Checklist items ---

    public List<NoteDetail.ChecklistItemResponse> listChecklistItems(Long noteId, UserPrincipal principal) {
        findVisibleNote(noteId, principal);
        return checklistItemRepository.findByNoteIdOrderBySortOrder(noteId)
                .stream().map(NoteDetail.ChecklistItemResponse::from).toList();
    }

    @Transactional
    public NoteDetail.ChecklistItemResponse addChecklistItem(Long noteId, ChecklistItemRequest req,
                                                              UserPrincipal principal) {
        Note note = findEditableNote(noteId, principal);
        long count = checklistItemRepository.countByNoteId(noteId);
        if (count >= 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MAX_CHECKLIST_ITEMS");

        ChecklistItem item = new ChecklistItem();
        item.setNote(note);
        item.setText(req.text());
        item.setChecked(req.isChecked() != null && req.isChecked());
        item.setSortOrder(req.sortOrder() != null ? req.sortOrder() : (int) count);
        checklistItemRepository.save(item);
        return NoteDetail.ChecklistItemResponse.from(item);
    }

    @Transactional
    public NoteDetail.ChecklistItemResponse updateChecklistItem(Long noteId, Long itemId,
                                                                 ChecklistItemRequest req,
                                                                 UserPrincipal principal) {
        findEditableNote(noteId, principal);
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("ChecklistItem", itemId));
        if (!item.getNote().getId().equals(noteId)) throw new EntityNotFoundException("ChecklistItem", itemId);

        if (req.text() != null) item.setText(req.text());
        if (req.isChecked() != null) item.setChecked(req.isChecked());
        if (req.sortOrder() != null) item.setSortOrder(req.sortOrder());
        checklistItemRepository.save(item);
        eventBus.publish(EventBus.Event.broadcast("ChecklistItemToggled",
                Map.of("noteId", noteId, "itemId", itemId, "checked", item.isChecked())));
        return NoteDetail.ChecklistItemResponse.from(item);
    }

    @Transactional
    public void deleteChecklistItem(Long noteId, Long itemId, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        findVisibleNote(noteId, principal);
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("ChecklistItem", itemId));
        if (!item.getNote().getId().equals(noteId)) throw new EntityNotFoundException("ChecklistItem", itemId);
        checklistItemRepository.delete(item);
    }

    @Transactional
    public List<NoteDetail.ChecklistItemResponse> reorderChecklistItems(Long noteId,
                                                                         ReorderRequest req,
                                                                         UserPrincipal principal) {
        findEditableNote(noteId, principal);
        List<ChecklistItem> items = checklistItemRepository.findByNoteIdOrderBySortOrder(noteId);
        Map<Long, Integer> orderMap = new java.util.HashMap<>();
        for (ReorderRequest.ReorderItem ri : req.items()) {
            orderMap.put(ri.id(), ri.sortOrder());
        }
        for (ChecklistItem item : items) {
            if (orderMap.containsKey(item.getId())) {
                item.setSortOrder(orderMap.get(item.getId()));
            }
        }
        checklistItemRepository.saveAll(items);
        return checklistItemRepository.findByNoteIdOrderBySortOrder(noteId)
                .stream().map(NoteDetail.ChecklistItemResponse::from).toList();
    }

    private Note findVisibleNote(Long id, UserPrincipal principal) {
        if (principal.isChild()) {
            return noteRepository.findByIdAndChildSafeAndDeletedAtIsNull(id, true)
                    .orElseThrow(() -> new EntityNotFoundException("Note", id));
        }
        return noteRepository.findActiveById(id).orElseThrow(() -> new EntityNotFoundException("Note", id));
    }

    private Note findEditableNote(Long noteId, UserPrincipal principal) {
        Note note = noteRepository.findActiveById(noteId)
                .orElseThrow(() -> new EntityNotFoundException("Note", noteId));
        if (principal.isChild() && note.getOwner().getId() != principal.getId()) {
            throw new ChildAccountWriteException();
        }
        return note;
    }

}
