package com.homekm.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CommentRequest(
        @NotBlank @Size(max = 4000) String body,
        /** User IDs to ping. Group mentions are expanded by the caller. */
        List<Long> mentionedUserIds,
        /** Group IDs to ping; expanded server-side and merged with mentionedUserIds. */
        List<Long> mentionedGroupIds
) {}
