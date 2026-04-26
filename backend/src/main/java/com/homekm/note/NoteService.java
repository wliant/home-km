package com.homekm.note;

import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.common.ChildAccountWriteException;
import com.homekm.common.ChildSafeService;
import com.homekm.common.EntityNotFoundException;
import com.homekm.common.PageResponse;
import com.homekm.folder.Folder;
import com.homekm.folder.FolderRepository;
import com.homekm.note.dto.*;
import com.homekm.reminder.ReminderRepository;
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

    private final NoteRepository noteRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ReminderRepository reminderRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final ChildSafeService childSafeService;

    public NoteService(NoteRepository noteRepository, ChecklistItemRepository checklistItemRepository,
                       ReminderRepository reminderRepository, FolderRepository folderRepository,
                       UserRepository userRepository, ChildSafeService childSafeService) {
        this.noteRepository = noteRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.reminderRepository = reminderRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.childSafeService = childSafeService;
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

        if (req.folderId() != null) {
            Folder folder = folderRepository.findById(req.folderId())
                    .orElseThrow(() -> new EntityNotFoundException("Folder", req.folderId()));
            note.setFolder(folder);
        }

        // Child: force child-safe; Adults: use provided value (default false)
        if (principal.isChild()) {
            note.setChildSafe(true);
        } else {
            note.setChildSafe(req.isChildSafe() != null && req.isChildSafe());
        }

        noteRepository.save(note);

        // If unsafe note added to safe folder, demote the folder
        if (!note.isChildSafe()) {
            childSafeService.demoteFolderIfNeeded(req.folderId(), false);
        }

        return NoteDetail.from(note, List.of(), List.of());
    }

    @Transactional
    public NoteDetail update(Long id, NoteRequest req, UserPrincipal principal) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note", id));

        // Children can only edit their own notes
        if (principal.isChild()) {
            if (note.getOwner().getId() != principal.getId()) {
                throw new ChildAccountWriteException();
            }
            // Children cannot change child-safe flag
        }

        if (req.title() != null) note.setTitle(req.title());
        if (req.body() != null) note.setBody(req.body());
        if (req.label() != null) note.setLabel(req.label());

        // Only adults can toggle child-safe
        if (!principal.isChild() && req.isChildSafe() != null) {
            note.setChildSafe(req.isChildSafe());
        }

        // Handle folder move
        if (req.folderId() != null) {
            Long currentFolderId = note.getFolder() != null ? note.getFolder().getId() : null;
            if (!req.folderId().equals(currentFolderId)) {
                Folder dest = folderRepository.findById(req.folderId())
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
        List<ChecklistItem> items = checklistItemRepository.findByNoteIdOrderBySortOrder(id);
        List<com.homekm.reminder.Reminder> reminders = reminderRepository.findByNoteId(id);
        return NoteDetail.from(note, items, reminders);
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note", id));
        noteRepository.delete(note);
    }

    @Transactional
    public NoteDetail pin(Long id, UserPrincipal principal) {
        Note note = findVisibleNote(id, principal);
        if (principal.isChild() && note.getOwner().getId() != principal.getId()) {
            throw new ChildAccountWriteException();
        }
        if (note.getPinnedAt() == null) {
            noteRepository.setPinnedAt(id, Instant.now());
            noteRepository.flush();
        }
        return getById(id, principal);
    }

    @Transactional
    public NoteDetail unpin(Long id, UserPrincipal principal) {
        Note note = findVisibleNote(id, principal);
        if (principal.isChild() && note.getOwner().getId() != principal.getId()) {
            throw new ChildAccountWriteException();
        }
        if (note.getPinnedAt() != null) {
            noteRepository.setPinnedAt(id, null);
            noteRepository.flush();
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
            return noteRepository.findByIdAndChildSafe(id, true)
                    .orElseThrow(() -> new EntityNotFoundException("Note", id));
        }
        return noteRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Note", id));
    }

    private Note findEditableNote(Long noteId, UserPrincipal principal) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new EntityNotFoundException("Note", noteId));
        if (principal.isChild() && note.getOwner().getId() != principal.getId()) {
            throw new ChildAccountWriteException();
        }
        return note;
    }
}
