package com.homekm.reminder;

import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.common.EntityNotFoundException;
import com.homekm.group.GroupService;
import com.homekm.note.Note;
import com.homekm.note.NoteRepository;
import com.homekm.note.dto.NoteDetail;
import com.homekm.reminder.dto.ReminderRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    public ReminderService(ReminderRepository reminderRepository, NoteRepository noteRepository,
                            UserRepository userRepository, GroupService groupService) {
        this.reminderRepository = reminderRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    /** Merge recipient user IDs with users expanded from any addressed groups. */
    private List<Long> resolveRecipients(ReminderRequest req) {
        Set<Long> ids = new HashSet<>();
        if (req.recipientUserIds() != null) ids.addAll(req.recipientUserIds());
        if (req.recipientGroupIds() != null && !req.recipientGroupIds().isEmpty()) {
            ids.addAll(groupService.expandGroupsToUserIds(req.recipientGroupIds()));
        }
        return new ArrayList<>(ids);
    }

    public List<NoteDetail.ReminderResponse> list(Long noteId) {
        return reminderRepository.findByNoteId(noteId).stream()
                .map(NoteDetail.ReminderResponse::from).toList();
    }

    @Transactional
    public NoteDetail.ReminderResponse create(Long noteId, ReminderRequest req, UserPrincipal principal) {
        Note note = noteRepository.findActiveById(noteId)
                .orElseThrow(() -> new EntityNotFoundException("Note", noteId));

        long count = reminderRepository.countByNoteId(noteId);
        if (count >= 10) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MAX_REMINDERS");

        Instant maxAhead = Instant.now().plus(5 * 365, ChronoUnit.DAYS);
        if (req.remindAt().isAfter(maxAhead)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "REMIND_AT_TOO_FAR");
        }

        Reminder reminder = new Reminder();
        reminder.setNote(note);
        reminder.setRemindAt(req.remindAt());
        reminder.setRecurrence(req.recurrence());
        reminderRepository.save(reminder);

        List<Long> resolved = resolveRecipients(req);
        if (!resolved.isEmpty()) {
            for (Long uid : resolved) {
                User user = userRepository.findById(uid)
                        .orElseThrow(() -> new EntityNotFoundException("User", uid));
                reminder.getRecipients().add(new ReminderRecipient(reminder, user));
            }
            reminderRepository.save(reminder);
        }

        return NoteDetail.ReminderResponse.from(reminder);
    }

    @Transactional
    public NoteDetail.ReminderResponse update(Long noteId, Long reminderId, ReminderRequest req) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new EntityNotFoundException("Reminder", reminderId));
        if (!reminder.getNote().getId().equals(noteId)) throw new EntityNotFoundException("Reminder", reminderId);

        reminder.setRemindAt(req.remindAt());
        reminder.setRecurrence(req.recurrence());
        reminder.setPushSent(false);
        reminder.getRecipients().clear();

        for (Long uid : resolveRecipients(req)) {
            User user = userRepository.findById(uid)
                    .orElseThrow(() -> new EntityNotFoundException("User", uid));
            reminder.getRecipients().add(new ReminderRecipient(reminder, user));
        }

        reminderRepository.save(reminder);
        return NoteDetail.ReminderResponse.from(reminder);
    }

    @Transactional
    public void delete(Long noteId, Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new EntityNotFoundException("Reminder", reminderId));
        if (!reminder.getNote().getId().equals(noteId)) throw new EntityNotFoundException("Reminder", reminderId);
        reminderRepository.delete(reminder);
    }
}
