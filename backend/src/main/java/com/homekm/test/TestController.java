package com.homekm.test;

import com.homekm.reminder.ReminderScheduler;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Profile("test")
class TestController {

    private final ReminderScheduler reminderScheduler;

    TestController(ReminderScheduler reminderScheduler) {
        this.reminderScheduler = reminderScheduler;
    }

    @PostMapping("/trigger-scheduler")
    public ResponseEntity<Void> triggerScheduler() {
        reminderScheduler.processDueReminders();
        return ResponseEntity.ok().build();
    }
}
