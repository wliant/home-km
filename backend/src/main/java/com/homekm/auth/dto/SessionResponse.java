package com.homekm.auth.dto;

import com.homekm.auth.RefreshToken;

import java.time.Instant;

public record SessionResponse(
        Long id,
        String deviceLabel,
        String userAgent,
        String ipAddress,
        Instant createdAt,
        Instant lastSeenAt,
        Instant expiresAt,
        boolean rememberMe,
        boolean current
) {
    public static SessionResponse from(RefreshToken rt, boolean current) {
        return new SessionResponse(rt.getId(), rt.getDeviceLabel(), rt.getUserAgent(),
                rt.getIpAddress(), rt.getCreatedAt(), rt.getLastSeenAt(), rt.getExpiresAt(),
                rt.isRememberMe(), current);
    }
}
