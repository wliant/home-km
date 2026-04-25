package com.homekm.note.dto;

import com.homekm.note.ChecklistItem;
import com.homekm.note.Note;
import com.homekm.reminder.Reminder;

import java.time.Instant;
import java.util.List;

public record NoteDetail(
        long id,
        Long folderId,
        long ownerId,
        String title,
        String body,
        String label,
        boolean isChildSafe,
        Instant createdAt,
        Instant updatedAt,
        List<ChecklistItemResponse> checklistItems,
        List<ReminderResponse> reminders
) {
    public record ChecklistItemResponse(long id, String text, boolean isChecked, int sortOrder,
                                        Instant createdAt, Instant updatedAt) {
        public static ChecklistItemResponse from(ChecklistItem item) {
            return new ChecklistItemResponse(item.getId(), item.getText(), item.isChecked(),
                    item.getSortOrder(), item.getCreatedAt(), item.getUpdatedAt());
        }
    }

    public record ReminderResponse(long id, Instant remindAt, String recurrence, boolean pushSent,
                                   List<Long> recipientUserIds, Instant createdAt, Instant updatedAt) {
        public static ReminderResponse from(Reminder r) {
            List<Long> recipients = r.getRecipients().stream()
                    .map(rr -> rr.getUser().getId())
                    .toList();
            return new ReminderResponse(r.getId(), r.getRemindAt(), r.getRecurrence(),
                    r.isPushSent(), recipients, r.getCreatedAt(), r.getUpdatedAt());
        }
    }

    public static NoteDetail from(Note note, List<ChecklistItem> items, List<Reminder> reminders) {
        return new NoteDetail(
                note.getId(),
                note.getFolder() != null ? note.getFolder().getId() : null,
                note.getOwner().getId(),
                note.getTitle(),
                note.getBody(),
                note.getLabel(),
                note.isChildSafe(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                items.stream().map(ChecklistItemResponse::from).toList(),
                reminders.stream().map(ReminderResponse::from).toList()
        );
    }
}
