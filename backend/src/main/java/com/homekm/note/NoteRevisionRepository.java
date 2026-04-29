package com.homekm.note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NoteRevisionRepository extends JpaRepository<NoteRevision, Long> {

    @Query("SELECT r FROM NoteRevision r WHERE r.note.id = :noteId ORDER BY r.editedAt DESC")
    List<NoteRevision> findByNoteIdOrderByEditedAtDesc(@Param("noteId") Long noteId);

    long countByNoteId(Long noteId);

    /**
     * Per-note retention cap. Drop everything older than the most recent
     * {@code keep} revisions for {@code noteId}. Combined with the
     * 90-day age cutoff in {@link #purgeOlderThan} this keeps the table
     * bounded.
     */
    @Modifying
    @Query(value = """
        DELETE FROM note_revisions
        WHERE note_id = :noteId
          AND id NOT IN (
            SELECT id FROM note_revisions
            WHERE note_id = :noteId
            ORDER BY edited_at DESC
            LIMIT :keep
          )
        """, nativeQuery = true)
    int trimToLast(@Param("noteId") Long noteId, @Param("keep") int keep);

    @Modifying
    @Query("DELETE FROM NoteRevision r WHERE r.editedAt < :cutoff")
    int purgeOlderThan(@Param("cutoff") Instant cutoff);
}
