package com.homekm.note.dto;

import com.homekm.note.Note;

import java.time.Instant;

public record NoteSummary(
        long id,
        Long folderId,
        long ownerId,
        String title,
        String label,
        boolean isChildSafe,
        long checklistItemCount,
        long checkedItemCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoteSummary from(Note note, long itemCount, long checkedCount) {
        return new NoteSummary(
                note.getId(),
                note.getFolder() != null ? note.getFolder().getId() : null,
                note.getOwner().getId(),
                note.getTitle(),
                note.getLabel(),
                note.isChildSafe(),
                itemCount,
                checkedCount,
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
