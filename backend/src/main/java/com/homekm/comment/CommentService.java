package com.homekm.comment;

import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.comment.dto.CommentRequest;
import com.homekm.comment.dto.CommentResponse;
import com.homekm.comment.dto.MentionInboxResponse;
import com.homekm.common.AccessControlService;
import com.homekm.common.EntityNotFoundException;
import com.homekm.common.Visibility;
import com.homekm.file.StoredFile;
import com.homekm.file.StoredFileRepository;
import com.homekm.group.GroupService;
import com.homekm.note.Note;
import com.homekm.note.NoteRepository;
import com.homekm.push.PushService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Comments + @-mentions on notes and files. Mention authors trigger:
 *   1) a row in {@code mention_inbox} per mentioned user (powers the bell badge)
 *   2) a Web Push to each mentioned user (so they hear about it without polling)
 * The comment author is filtered out of their own mention list — pinging
 * yourself is rarely useful.
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final MentionInboxRepository mentionInboxRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final PushService pushService;
    private final NoteRepository noteRepository;
    private final StoredFileRepository storedFileRepository;
    private final AccessControlService accessControlService;

    public CommentService(CommentRepository commentRepository,
                          MentionInboxRepository mentionInboxRepository,
                          UserRepository userRepository,
                          GroupService groupService,
                          PushService pushService,
                          NoteRepository noteRepository,
                          StoredFileRepository storedFileRepository,
                          AccessControlService accessControlService) {
        this.commentRepository = commentRepository;
        this.mentionInboxRepository = mentionInboxRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
        this.pushService = pushService;
        this.noteRepository = noteRepository;
        this.storedFileRepository = storedFileRepository;
        this.accessControlService = accessControlService;
    }

    /**
     * Resolve the underlying note/file and check the principal can see it.
     * Throws 404 if the item doesn't exist (avoid leaking which IDs exist),
     * 403 if it exists but the caller lacks read access, and additionally
     * blocks child accounts from non-child-safe items — same rule notes/files
     * already enforce in their own controllers.
     */
    private void requireReadAccess(Comment.ItemType itemType, Long itemId, UserPrincipal principal) {
        if (itemType == Comment.ItemType.note) {
            Note n = noteRepository.findActiveById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("Note", itemId));
            if (principal.isChild() && !n.isChildSafe()) {
                throw new AccessDeniedException("not allowed");
            }
            Long ownerId = n.getOwner() != null ? n.getOwner().getId() : null;
            if (!accessControlService.canRead("note", n.getId(), ownerId,
                    Visibility.fromDb(n.getVisibility()), principal)) {
                throw new AccessDeniedException("not allowed");
            }
        } else {
            StoredFile f = storedFileRepository.findActiveById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("File", itemId));
            if (principal.isChild() && !f.isChildSafe()) {
                throw new AccessDeniedException("not allowed");
            }
            Long ownerId = f.getOwner() != null ? f.getOwner().getId() : null;
            if (!accessControlService.canRead("file", f.getId(), ownerId,
                    Visibility.fromDb(f.getVisibility()), principal)) {
                throw new AccessDeniedException("not allowed");
            }
        }
    }

    /** Same as read, plus a write-permission check (so children are blocked outright). */
    private void requireWriteAccess(Comment.ItemType itemType, Long itemId, UserPrincipal principal) {
        if (itemType == Comment.ItemType.note) {
            Note n = noteRepository.findActiveById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("Note", itemId));
            if (principal.isChild() && !n.isChildSafe()) {
                throw new AccessDeniedException("not allowed");
            }
            Long ownerId = n.getOwner() != null ? n.getOwner().getId() : null;
            if (!accessControlService.canWrite("note", n.getId(), ownerId,
                    Visibility.fromDb(n.getVisibility()), principal)) {
                throw new AccessDeniedException("not allowed");
            }
        } else {
            StoredFile f = storedFileRepository.findActiveById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("File", itemId));
            if (principal.isChild() && !f.isChildSafe()) {
                throw new AccessDeniedException("not allowed");
            }
            Long ownerId = f.getOwner() != null ? f.getOwner().getId() : null;
            if (!accessControlService.canWrite("file", f.getId(), ownerId,
                    Visibility.fromDb(f.getVisibility()), principal)) {
                throw new AccessDeniedException("not allowed");
            }
        }
    }

    public List<CommentResponse> list(Comment.ItemType itemType, Long itemId, UserPrincipal principal) {
        requireReadAccess(itemType, itemId, principal);
        return commentRepository.findByItem(itemType, itemId).stream()
                .map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse create(Comment.ItemType itemType, Long itemId, CommentRequest req,
                                   UserPrincipal principal) {
        requireWriteAccess(itemType, itemId, principal);
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", principal.getId()));

        Comment c = new Comment();
        c.setItemType(itemType);
        c.setItemId(itemId);
        c.setAuthor(author);
        c.setBody(req.body());

        Set<Long> mentionUserIds = resolveMentionUserIds(req, author.getId());
        for (Long uid : mentionUserIds) {
            User u = userRepository.findById(uid)
                    .orElseThrow(() -> new EntityNotFoundException("User", uid));
            c.getMentionedUsers().add(u);
        }
        commentRepository.save(c);

        // Inbox + push fan-out. Each pinged user gets one inbox row even if
        // mentioned through multiple groups.
        for (Long uid : mentionUserIds) {
            MentionInbox mi = new MentionInbox();
            mi.setUserId(uid);
            mi.setComment(c);
            mentionInboxRepository.save(mi);
        }
        if (!mentionUserIds.isEmpty()) {
            String url = "/" + (itemType == Comment.ItemType.note ? "notes" : "files") + "/" + itemId;
            String preview = c.getBody().length() > 80 ? c.getBody().substring(0, 80) + "…" : c.getBody();
            pushService.sendToUsers(new java.util.ArrayList<>(mentionUserIds),
                    author.getDisplayName() + " mentioned you", preview, url);
        }
        return CommentResponse.from(c);
    }

    @Transactional
    public CommentResponse update(Long commentId, CommentRequest req, UserPrincipal principal) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment", commentId));
        if (c.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "COMMENT_DELETED");
        }
        if (!c.getAuthor().getId().equals(principal.getId())) {
            throw new AccessDeniedException("only the author may edit");
        }
        c.setBody(req.body());
        c.setEditedAt(Instant.now());
        // We don't reshuffle mentions on edit — pinging again on each keystroke
        // would spam. Initial fan-out is the only push.
        return CommentResponse.from(commentRepository.save(c));
    }

    @Transactional
    public void delete(Long commentId, UserPrincipal principal) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment", commentId));
        boolean isAuthor = c.getAuthor().getId().equals(principal.getId());
        if (!isAuthor && !principal.isAdmin()) {
            throw new AccessDeniedException("only the author or an admin may delete");
        }
        c.setDeletedAt(Instant.now());
        commentRepository.save(c);
    }

    public List<MentionInboxResponse> inbox(Long userId) {
        return mentionInboxRepository.findByUserIdNewestFirst(userId).stream()
                .map(MentionInboxResponse::from).toList();
    }

    public long unreadCount(Long userId) {
        return mentionInboxRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markRead(Long inboxId, Long userId) {
        MentionInbox m = mentionInboxRepository.findByIdAndUserId(inboxId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Mention", inboxId));
        if (m.getReadAt() == null) {
            m.setReadAt(Instant.now());
            mentionInboxRepository.save(m);
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        for (MentionInbox m : mentionInboxRepository.findByUserIdNewestFirst(userId)) {
            if (m.getReadAt() == null) {
                m.setReadAt(Instant.now());
                mentionInboxRepository.save(m);
            }
        }
    }

    private Set<Long> resolveMentionUserIds(CommentRequest req, Long excludeUserId) {
        Set<Long> ids = new HashSet<>();
        if (req.mentionedUserIds() != null) ids.addAll(req.mentionedUserIds());
        if (req.mentionedGroupIds() != null && !req.mentionedGroupIds().isEmpty()) {
            ids.addAll(groupService.expandGroupsToUserIds(req.mentionedGroupIds()));
        }
        ids.remove(excludeUserId);
        return ids;
    }
}
