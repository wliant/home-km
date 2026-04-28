package com.homekm.reminder;

import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.common.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/reminders")
public class IcsExportController {

    private static final DateTimeFormatter ICS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final UserRepository userRepository;
    private final ReminderRepository reminderRepository;

    public IcsExportController(UserRepository userRepository, ReminderRepository reminderRepository) {
        this.userRepository = userRepository;
        this.reminderRepository = reminderRepository;
    }

    @GetMapping(value = "/me.ics", produces = "text/calendar")
    public ResponseEntity<byte[]> ics(@RequestParam("token") String token) {
        User u = userRepository.findByIcsToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INVALID_TOKEN"));
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//homekm//ics//EN\r\nCALSCALE:GREGORIAN\r\n");
        ZoneId zone = ZoneId.of(u.getTimezone() == null ? "UTC" : u.getTimezone());
        for (Reminder r : reminderRepository.findAll()) {
            boolean owns = r.getNote() != null && r.getNote().getOwner() != null
                    && r.getNote().getOwner().getId().equals(u.getId());
            boolean recipient = r.getRecipients().stream().anyMatch(rc ->
                    rc.getUser() != null && rc.getUser().getId().equals(u.getId()));
            if (!owns && !recipient) continue;
            if (r.getRemindAt() == null) continue;
            ZonedDateTime z = r.getRemindAt().atZone(zone);
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:reminder-").append(r.getId()).append("@homekm\r\n");
            sb.append("DTSTAMP:").append(z.withZoneSameInstant(ZoneId.of("UTC")).format(ICS_FMT)).append("\r\n");
            sb.append("DTSTART:").append(z.withZoneSameInstant(ZoneId.of("UTC")).format(ICS_FMT)).append("\r\n");
            String summary = r.getNote() != null ? r.getNote().getTitle() : "Reminder";
            sb.append("SUMMARY:").append(escape(summary)).append("\r\n");
            sb.append("END:VEVENT\r\n");
        }
        sb.append("END:VCALENDAR\r\n");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/me.ics/regenerate")
    @Transactional
    public ResponseEntity<TokenResponse> regenerate(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
        User u = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", principal.getId()));
        String token = UUID.randomUUID().toString().replace("-", "");
        u.setIcsToken(token);
        userRepository.save(u);
        return ResponseEntity.ok(new TokenResponse(token));
    }

    public record TokenResponse(String token) {}

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
