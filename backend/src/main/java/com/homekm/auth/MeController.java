package com.homekm.auth;

import com.homekm.reminder.ReminderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Endpoints describing the calling user without going through the auth flow.
 * The auth payload is in {@link AuthController} ({@code /api/auth/me}); this
 * lives at {@code /api/me} for ambient state polled by the UI.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final ReminderRepository reminderRepository;

    public MeController(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    public record UnreadResponse(long count) {}

    /**
     * Number of reminders that have already fired for the calling user.
     * Source for {@code navigator.setAppBadge}.
     */
    @GetMapping("/unread")
    public ResponseEntity<UnreadResponse> unread(@AuthenticationPrincipal UserPrincipal principal) {
        long count = reminderRepository.countUnreadForUser(principal.getId(), Instant.now());
        return ResponseEntity.ok(new UnreadResponse(count));
    }
}
