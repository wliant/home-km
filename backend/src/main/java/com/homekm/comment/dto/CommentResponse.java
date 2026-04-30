package com.homekm.comment.dto;

import com.homekm.comment.Comment;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
        Long id,
        String itemType,
        Long itemId,
        Long authorId,
        String authorDisplayName,
        String body,
        List<Long> mentionedUserIds,
        Instant createdAt,
        Instant editedAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getItemType().name(),
                c.getItemId(),
                c.getAuthor() != null ? c.getAuthor().getId() : null,
                c.getAuthor() != null ? c.getAuthor().getDisplayName() : null,
                c.getBody(),
                c.getMentionedUsers().stream().map(u -> u.getId()).toList(),
                c.getCreatedAt(),
                c.getEditedAt());
    }
}
