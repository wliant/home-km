package com.homekm.reminder;

import com.homekm.auth.UserPrincipal;
import com.homekm.note.dto.NoteDetail;
import com.homekm.reminder.dto.ReminderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes/{noteId}/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    public ResponseEntity<List<NoteDetail.ReminderResponse>> list(@PathVariable Long noteId) {
        return ResponseEntity.ok(reminderService.list(noteId));
    }

    @PostMapping
    public ResponseEntity<NoteDetail.ReminderResponse> create(
            @PathVariable Long noteId,
            @Valid @RequestBody ReminderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reminderService.create(noteId, req, principal));
    }

    @PutMapping("/{reminderId}")
    public ResponseEntity<NoteDetail.ReminderResponse> update(
            @PathVariable Long noteId, @PathVariable Long reminderId,
            @Valid @RequestBody ReminderRequest req) {
        return ResponseEntity.ok(reminderService.update(noteId, reminderId, req));
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> delete(@PathVariable Long noteId, @PathVariable Long reminderId) {
        reminderService.delete(noteId, reminderId);
        return ResponseEntity.noContent().build();
    }
}
