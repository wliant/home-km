package com.homekm.reminder;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/reminders")
public class ReminderSnoozeController {

    private final ReminderRepository reminderRepository;

    public ReminderSnoozeController(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @PostMapping("/{id}/snooze")
    @Transactional
    public ResponseEntity<SnoozeResponse> snooze(@PathVariable Long id,
                                                   @Valid @RequestBody SnoozeRequest req,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("login required");
        Reminder r = reminderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reminder", id));
        Instant now = Instant.now();
        Instant base = r.getRemindAt() != null && r.getRemindAt().isAfter(now) ? r.getRemindAt() : now;
        r.setRemindAt(base.plus(req.minutes(), ChronoUnit.MINUTES));
        r.setPushSent(false);
        reminderRepository.save(r);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new SnoozeResponse(r.getId(), r.getRemindAt()));
    }

    public record SnoozeRequest(@NotNull @Min(1) Integer minutes) {}
    public record SnoozeResponse(Long id, Instant remindAt) {}
}
