package com.homekm.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homekm.reminder.ReminderRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
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
    private final DataExportService dataExportService;
    private final DataExportRepository dataExportRepository;

    public MeController(ReminderRepository reminderRepository,
                         UserRepository userRepository,
                         ObjectMapper objectMapper,
                         DataExportService dataExportService,
                         DataExportRepository dataExportRepository) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.dataExportService = dataExportService;
        this.dataExportRepository = dataExportRepository;
    }

    public record ExportResponse(
            Long id,
            String status,
            Long sizeBytes,
            Instant createdAt,
            Instant readyAt,
            Instant expiresAt,
            String downloadUrl,
            String errorMessage) {}

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

    public record InboxReminder(
            long id, long noteId, String noteTitle, Instant remindAt, String recurrence, boolean fired) {}

    /**
     * Reminders the user is a recipient on or owns. Drives the inbox view —
     * one screen for all upcoming + recently-fired reminders so users don't
     * have to walk every note to find what's pending.
     */
    @GetMapping("/reminders")
    public ResponseEntity<List<InboxReminder>> myReminders(@AuthenticationPrincipal UserPrincipal principal) {
        Instant now = Instant.now();
        List<InboxReminder> rows = reminderRepository.findAllForUser(principal.getId()).stream()
                .map(r -> new InboxReminder(
                        r.getId(),
                        r.getNote().getId(),
                        r.getNote().getTitle(),
                        r.getRemindAt(),
                        r.getRecurrence(),
                        r.getRemindAt().isBefore(now)))
                .toList();
        return ResponseEntity.ok(rows);
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

    /**
     * Enqueue a GDPR-style data export. Returns the new request in PENDING
     * state; the background job assembles the ZIP within a poll interval.
     */
    @PostMapping("/export")
    public ResponseEntity<ExportResponse> requestExport(@AuthenticationPrincipal UserPrincipal principal) {
        DataExportRequest req = dataExportService.enqueue(principal.getId());
        return ResponseEntity.ok(toResponse(req));
    }

    /** All export requests for the calling user, newest first. */
    @GetMapping("/export")
    public ResponseEntity<List<ExportResponse>> listExports(@AuthenticationPrincipal UserPrincipal principal) {
        List<ExportResponse> responses = dataExportRepository.findByUserId(principal.getId()).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /** Single export — when READY the response carries a 15-minute presigned download URL. */
    @GetMapping("/export/{id}")
    public ResponseEntity<ExportResponse> getExport(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return dataExportRepository.findByIdAndUserId(id, principal.getId())
                .map(req -> ResponseEntity.ok(toResponse(req)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ExportResponse toResponse(DataExportRequest req) {
        String url = null;
        try {
            url = dataExportService.presignedDownloadUrl(req);
        } catch (Exception ignored) {
            // surfaced as null download URL
        }
        return new ExportResponse(
                req.getId(),
                req.getStatus().name(),
                req.getSizeBytes(),
                req.getCreatedAt(),
                req.getReadyAt(),
                req.getExpiresAt(),
                url,
                req.getErrorMessage());
    }

    public record QuietHoursResponse(String start, String end, String timezone) {}
    public record QuietHoursRequest(String start, String end, String timezone) {}

    /**
     * Per-user quiet-hours window. Times serialize as "HH:mm" wall-clock in
     * the user's timezone. Both null = quiet hours off; setting both to the
     * same value is treated as off too. Wrapping windows (start &gt; end)
     * are valid and cross midnight.
     */
    @GetMapping("/quiet-hours")
    public ResponseEntity<QuietHoursResponse> getQuietHours(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(new QuietHoursResponse(
                user.getQuietHoursStart() != null ? user.getQuietHoursStart().toString() : null,
                user.getQuietHoursEnd() != null ? user.getQuietHoursEnd().toString() : null,
                user.getTimezone()));
    }

    @PutMapping("/quiet-hours")
    @Transactional
    public ResponseEntity<QuietHoursResponse> setQuietHours(
            @RequestBody QuietHoursRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        // Both null = clear the window. Otherwise both must be present.
        boolean clearing = (req.start() == null || req.start().isBlank())
                && (req.end() == null || req.end().isBlank());
        if (clearing) {
            user.setQuietHoursStart(null);
            user.setQuietHoursEnd(null);
        } else {
            try {
                user.setQuietHoursStart(LocalTime.parse(req.start()));
                user.setQuietHoursEnd(LocalTime.parse(req.end()));
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "INVALID_TIME: expected HH:mm");
            }
        }
        if (req.timezone() != null && !req.timezone().isBlank()) {
            try {
                ZoneId.of(req.timezone());
                user.setTimezone(req.timezone());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "INVALID_TIMEZONE: " + req.timezone());
            }
        }
        userRepository.save(user);
        return ResponseEntity.ok(new QuietHoursResponse(
                user.getQuietHoursStart() != null ? user.getQuietHoursStart().toString() : null,
                user.getQuietHoursEnd() != null ? user.getQuietHoursEnd().toString() : null,
                user.getTimezone()));
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
