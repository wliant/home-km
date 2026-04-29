package com.homekm.admin;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.EntityNotFoundException;
import com.homekm.file.StoredFile;
import com.homekm.file.StoredFileRepository;
import com.homekm.note.Note;
import com.homekm.note.NoteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Surface for the parental review queue. Lists notes/files an admin hasn't
 * yet ruled on (no {@code child_safe_review_at}); admin actions stamp it.
 * Items added by another adult that should be visible to children are the
 * primary case — a parent reviews and toggles {@code is_child_safe} via
 * the existing per-item endpoint, then marks reviewed here.
 */
@RestController
@RequestMapping("/api/admin/review-queue")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final EntityManager em;
    private final NoteRepository noteRepository;
    private final StoredFileRepository fileRepository;

    public AdminReviewController(EntityManager em,
                                  NoteRepository noteRepository,
                                  StoredFileRepository fileRepository) {
        this.em = em;
        this.noteRepository = noteRepository;
        this.fileRepository = fileRepository;
    }

    public record QueueItem(String type, long id, String title, boolean isChildSafe,
                            long ownerId, Instant createdAt) {}

    @GetMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<List<QueueItem>> list() {
        List<QueueItem> items = new ArrayList<>();
        // Notes
        Query notes = em.createNativeQuery("""
                SELECT id, title, is_child_safe, owner_id, created_at FROM notes
                WHERE child_safe_review_at IS NULL AND deleted_at IS NULL AND is_template = false
                ORDER BY created_at DESC LIMIT 100
                """);
        for (Object[] r : (List<Object[]>) notes.getResultList()) {
            items.add(new QueueItem("note",
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    (Boolean) r[2],
                    ((Number) r[3]).longValue(),
                    ((java.sql.Timestamp) r[4]).toInstant()));
        }
        // Files
        Query files = em.createNativeQuery("""
                SELECT id, filename, is_child_safe, owner_id, uploaded_at FROM files
                WHERE child_safe_review_at IS NULL AND deleted_at IS NULL
                ORDER BY uploaded_at DESC LIMIT 100
                """);
        for (Object[] r : (List<Object[]>) files.getResultList()) {
            items.add(new QueueItem("file",
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    (Boolean) r[2],
                    ((Number) r[3]).longValue(),
                    ((java.sql.Timestamp) r[4]).toInstant()));
        }
        items.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
        return ResponseEntity.ok(items);
    }

    public record ReviewRequest(boolean childSafe) {}

    @PostMapping("/note/{id}")
    @Transactional
    public ResponseEntity<Void> reviewNote(@PathVariable Long id,
                                            @RequestBody ReviewRequest req,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        Note note = noteRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note", id));
        note.setChildSafe(req.childSafe());
        note.setChildSafeReviewAt(Instant.now());
        noteRepository.save(note);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/file/{id}")
    @Transactional
    public ResponseEntity<Void> reviewFile(@PathVariable Long id,
                                            @RequestBody ReviewRequest req,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        StoredFile file = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File", id));
        file.setChildSafe(req.childSafe());
        file.setChildSafeReviewAt(Instant.now());
        fileRepository.save(file);
        return ResponseEntity.noContent().build();
    }
}
