package com.homekm.reminder;

import com.homekm.auth.User;
import com.homekm.auth.UserRepository;
import com.homekm.note.Note;
import com.homekm.push.PushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock ReminderRepository reminderRepository;
    @Mock UserRepository userRepository;
    @Mock PushService pushService;

    @InjectMocks ReminderScheduler scheduler;

    private User owner;
    private Note note;

    @BeforeEach
    void setUp() {
        owner = new User();
        ReflectionTestUtils.setField(owner, "id", 1L);

        note = new Note();
        ReflectionTestUtils.setField(note, "id", 10L);
        note.setTitle("Shopping list");
        note.setOwner(owner);
    }

    @Test
    void noDueReminders_doesNothing() {
        when(reminderRepository.findDueReminders(any())).thenReturn(List.of());

        scheduler.processDueReminders();

        verify(pushService, never()).sendToUsers(any(), any(), any(), any());
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void oneTimeReminder_markedSentAndNotAdvanced() {
        Reminder reminder = buildReminder(null); // no recurrence
        when(reminderRepository.findDueReminders(any())).thenReturn(List.of(reminder));

        Instant originalRemindAt = reminder.getRemindAt();
        scheduler.processDueReminders();

        assertThat(reminder.isPushSent()).isTrue();
        assertThat(reminder.getRemindAt()).isEqualTo(originalRemindAt);
        verify(reminderRepository).save(reminder);
        verify(pushService).sendToUsers(eq(List.of(1L)), anyString(), anyString(), anyString());
    }

    @Test
    void dailyRecurrence_advancesRemindAtByOneDay() {
        Reminder reminder = buildReminder("daily");
        Instant original = reminder.getRemindAt();
        when(reminderRepository.findDueReminders(any())).thenReturn(List.of(reminder));

        scheduler.processDueReminders();

        assertThat(reminder.getRemindAt()).isEqualTo(original.plus(1, ChronoUnit.DAYS));
        assertThat(reminder.isPushSent()).isFalse(); // reset for next firing
    }

    @Test
    void weeklyRecurrence_advancesRemindAtBySevenDays() {
        Reminder reminder = buildReminder("weekly");
        Instant original = reminder.getRemindAt();
        when(reminderRepository.findDueReminders(any())).thenReturn(List.of(reminder));

        scheduler.processDueReminders();

        assertThat(reminder.getRemindAt()).isEqualTo(original.plus(7, ChronoUnit.DAYS));
        assertThat(reminder.isPushSent()).isFalse();
    }

    @Test
    void monthlyRecurrence_advancesBy30Days() {
        Reminder reminder = buildReminder("monthly");
        Instant original = reminder.getRemindAt();
        when(reminderRepository.findDueReminders(any())).thenReturn(List.of(reminder));

        scheduler.processDueReminders();

        assertThat(reminder.getRemindAt()).isEqualTo(original.plus(30, ChronoUnit.DAYS));
    }

    @Test
    void yearlyRecurrence_advancesBy365Days() {
        Reminder reminder = buildReminder("yearly");
        Instant original = reminder.getRemindAt();
        when(reminderRepository.findDueReminders(any())).thenReturn(List.of(reminder));

        scheduler.processDueReminders();

        assertThat(reminder.getRemindAt()).isEqualTo(original.plus(365, ChronoUnit.DAYS));
    }

    @Test
    void pushSendsToNoteOwner_whenNoExplicitRecipients() {
        Reminder reminder = buildReminder(null);
        when(reminderRepository.findDueReminders(any())).thenReturn(List.of(reminder));

        scheduler.processDueReminders();

        verify(pushService).sendToUsers(
                eq(List.of(1L)),
                contains("Shopping list"),
                anyString(),
                contains("/notes/10")
        );
    }

    private Reminder buildReminder(String recurrence) {
        Reminder r = new Reminder();
        ReflectionTestUtils.setField(r, "id", 100L);
        r.setNote(note);
        r.setRemindAt(Instant.now().minusSeconds(60));
        r.setRecurrence(recurrence);
        r.setPushSent(false);
        return r;
    }
}
