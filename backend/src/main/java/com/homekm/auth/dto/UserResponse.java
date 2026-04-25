package com.homekm.auth.dto;

import com.homekm.auth.User;

import java.time.Instant;

public record UserResponse(
        long id,
        String email,
        String displayName,
        boolean isAdmin,
        boolean isChild,
        boolean isActive,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isAdmin(),
                user.isChild(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
