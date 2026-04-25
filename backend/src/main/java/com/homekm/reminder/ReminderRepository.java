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
}
