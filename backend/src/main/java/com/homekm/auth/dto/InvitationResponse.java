package com.homekm.auth.dto;

import com.homekm.auth.Invitation;

import java.time.Instant;

public record InvitationResponse(
        Long id,
        String email,
        String role,
        Instant createdAt,
        Instant expiresAt,
        Instant acceptedAt,
        boolean accepted,
        boolean expired
) {
    public static InvitationResponse from(Invitation inv) {
        return new InvitationResponse(inv.getId(), inv.getEmail(), inv.getRole(),
                inv.getCreatedAt(), inv.getExpiresAt(), inv.getAcceptedAt(),
                inv.isAccepted(), inv.isExpired());
    }
}
