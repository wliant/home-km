package com.homekm.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homekm.reminder.ReminderRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Endpoints describing the calling user without going through the auth flow.
 * The auth payload is in {@link AuthController} ({@code /api/auth/me}); this
 * lives at {@code /api/me} for ambient state polled by the UI.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public MeController(ReminderRepository reminderRepository,
                         UserRepository userRepository,
                         ObjectMapper objectMapper) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
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

    /**
     * Per-user notification routing. Currently consumed by {@code PushService}
     * to filter reminder push delivery; future event types (mentions, share
     * invites) plug in by adding new keys to the JSON without a schema bump.
     */
    @GetMapping("/notification-prefs")
    public ResponseEntity<Map<String, Object>> getNotificationPrefs(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(parsePrefs(user.getNotificationPrefs()));
    }

    @PutMapping("/notification-prefs")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateNotificationPrefs(
            @RequestBody Map<String, Object> prefs,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        try {
            user.setNotificationPrefs(objectMapper.writeValueAsString(prefs));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid notification preferences", e);
        }
        userRepository.save(user);
        return ResponseEntity.ok(prefs);
    }

    private Map<String, Object> parsePrefs(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            return parsed;
        } catch (IOException e) {
            return Map.of();
        }
    }
}
