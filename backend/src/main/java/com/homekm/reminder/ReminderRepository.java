package com.homekm.reminder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByNoteId(Long noteId);

    long countByNoteId(Long noteId);

    @Query("SELECT r FROM Reminder r WHERE r.pushSent = false AND r.remindAt <= :now")
    List<Reminder> findDueReminders(Instant now);

    /**
     * Count reminders that the given user should see as pending — already due
     * (remindAt &lt;= now) and the user is a recipient. Source for the
     * navigator.setAppBadge count.
     */
    @Query("""
            SELECT COUNT(r) FROM Reminder r JOIN r.recipients rr
            WHERE rr.user.id = :userId AND r.remindAt <= :now
            """)
    long countUnreadForUser(Long userId, Instant now);

    /**
     * Reminders the user is a recipient on, ordered by remindAt ascending.
     * Includes recurring rows (still useful — remindAt has been advanced past
     * the last fire so it surfaces as the next occurrence).
     */
    @Query("""
            SELECT DISTINCT r FROM Reminder r LEFT JOIN r.recipients rr
            WHERE (rr.user.id = :userId OR r.note.owner.id = :userId)
            ORDER BY r.remindAt ASC
            """)
    List<Reminder> findAllForUser(Long userId);
}
