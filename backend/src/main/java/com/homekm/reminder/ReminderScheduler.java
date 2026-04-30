package com.homekm.reminder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homekm.auth.UserRepository;
import com.homekm.common.OutboxEvent;
import com.homekm.common.OutboxRepository;
import com.homekm.common.ShutdownState;
import com.homekm.push.PushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final PushService pushService;
    private final ShutdownState shutdownState;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ReminderScheduler(ReminderRepository reminderRepository,
                               UserRepository userRepository,
                               PushService pushService,
                               @Autowired(required = false) ShutdownState shutdownState,
                               OutboxRepository outboxRepository,
                               ObjectMapper objectMapper) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.pushService = pushService;
        this.shutdownState = shutdownState;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    private boolean shuttingDown() {
        return shutdownState != null && shutdownState.isShuttingDown();
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void processDueReminders() {
        if (shuttingDown()) {
            log.debug("skipping reminder tick; shutdown in progress");
            return;
        }
        List<Reminder> due = reminderRepository.findDueReminders(Instant.now());
        for (Reminder reminder : due) {
            if (shuttingDown()) break;
            try {
                // Write the push intent into the outbox in the same tx as the
                // reminder mutation. {@link com.homekm.common.OutboxPublisher}
                // does the actual push delivery on its own polling loop, so a
                // crash between "marked sent" and "actually pushed" no longer
                // drops the notification.
                enqueuePushOutbox(reminder);
                reminder.setPushSent(true);
                if (reminder.getRecurrence() != null) {
                    reminder.setRemindAt(advanceRemindAt(reminder.getRemindAt(), reminder.getRecurrence()));
                    reminder.setPushSent(false);
                }
                reminderRepository.save(reminder);
            } catch (Exception e) {
                log.error("Failed to process reminder {}: {}", reminder.getId(), e.getMessage());
            }
        }
    }

    private void enqueuePushOutbox(Reminder reminder) throws Exception {
        List<Long> recipientIds;
        if (reminder.getRecipients().isEmpty()) {
            recipientIds = List.of(reminder.getNote().getOwner().getId());
        } else {
            recipientIds = reminder.getRecipients().stream().map(r -> r.getUser().getId()).toList();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Reminder: " + reminder.getNote().getTitle());
        payload.put("body", reminder.getNote().getTitle());
        payload.put("url", "/notes/" + reminder.getNote().getId());
        payload.put("reminderId", reminder.getId());

        OutboxEvent event = new OutboxEvent();
        event.setEventType("REMINDER_PUSH");
        event.setPayload(objectMapper.writeValueAsString(payload));
        event.setUserIds(recipientIds.toArray(new Long[0]));
        outboxRepository.save(event);
    }

    /**
     * Advance the next-fire timestamp using either an iCalendar RRULE
     * string (post V024) or a legacy enum value (pre V024 — kept for the
     * narrow window where a row written before the migration could still
     * be in flight). Falls back to the original timestamp on parse failure
     * so a malformed RRULE never crashes the scheduler.
     */
    private Instant advanceRemindAt(Instant remindAt, String recurrence) {
        if (recurrence == null || recurrence.isBlank()) return remindAt;
        if (recurrence.startsWith("FREQ=")) {
            try {
                net.fortuna.ical4j.model.Recur recur = new net.fortuna.ical4j.model.Recur(recurrence);
                net.fortuna.ical4j.model.DateTime seed = new net.fortuna.ical4j.model.DateTime(java.util.Date.from(remindAt));
                seed.setUtc(true);
                net.fortuna.ical4j.model.DateTime after = new net.fortuna.ical4j.model.DateTime(seed.getTime() + 1000L);
                after.setUtc(true);
                net.fortuna.ical4j.model.Date next = recur.getNextDate(seed, after);
                return next != null ? next.toInstant() : remindAt;
            } catch (Exception e) {
                log.warn("Bad RRULE '{}' on reminder advance: {}", recurrence, e.getMessage());
                return remindAt;
            }
        }
        // Legacy values, kept until rolling deploy clears stragglers.
        return switch (recurrence) {
            case "daily" -> remindAt.plus(1, ChronoUnit.DAYS);
            case "weekly" -> remindAt.plus(7, ChronoUnit.DAYS);
            case "monthly" -> remindAt.plus(30, ChronoUnit.DAYS);
            case "yearly" -> remindAt.plus(365, ChronoUnit.DAYS);
            default -> remindAt;
        };
    }
}
