package com.homekm.comment.dto;

import com.homekm.comment.MentionInbox;

import java.time.Instant;

public record MentionInboxResponse(
        Long id,
        Long commentId,
        String itemType,
        Long itemId,
        Long authorId,
        String authorDisplayName,
        String preview,
        Instant readAt,
        Instant createdAt
) {
    private static final int PREVIEW_CHARS = 160;

    public static MentionInboxResponse from(MentionInbox m) {
        var c = m.getComment();
        String body = c != null ? c.getBody() : "";
        String preview = body.length() > PREVIEW_CHARS ? body.substring(0, PREVIEW_CHARS) + "…" : body;
        return new MentionInboxResponse(
                m.getId(),
                c != null ? c.getId() : null,
                c != null ? c.getItemType().name() : null,
                c != null ? c.getItemId() : null,
                c != null && c.getAuthor() != null ? c.getAuthor().getId() : null,
                c != null && c.getAuthor() != null ? c.getAuthor().getDisplayName() : null,
                preview,
                m.getReadAt(),
                m.getCreatedAt());
    }
}
