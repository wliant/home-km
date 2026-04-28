package com.homekm.reminder;

import com.homekm.auth.UserRepository;
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
import java.util.List;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final PushService pushService;
    private final ShutdownState shutdownState;

    public ReminderScheduler(ReminderRepository reminderRepository,
                               UserRepository userRepository,
                               PushService pushService,
                               @Autowired(required = false) ShutdownState shutdownState) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.pushService = pushService;
        this.shutdownState = shutdownState;
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
                sendPush(reminder);
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

    private void sendPush(Reminder reminder) {
        List<Long> recipientIds;
        if (reminder.getRecipients().isEmpty()) {
            recipientIds = List.of(reminder.getNote().getOwner().getId());
        } else {
            recipientIds = reminder.getRecipients().stream().map(r -> r.getUser().getId()).toList();
        }

        String title = "Reminder: " + reminder.getNote().getTitle();
        String body = reminder.getNote().getTitle();
        String url = "/notes/" + reminder.getNote().getId();
        pushService.sendToUsers(recipientIds, title, body, url);
    }

    private Instant advanceRemindAt(Instant remindAt, String recurrence) {
        return switch (recurrence) {
            case "daily" -> remindAt.plus(1, ChronoUnit.DAYS);
            case "weekly" -> remindAt.plus(7, ChronoUnit.DAYS);
            case "monthly" -> remindAt.plus(30, ChronoUnit.DAYS);
            case "yearly" -> remindAt.plus(365, ChronoUnit.DAYS);
            default -> remindAt;
        };
    }
}
