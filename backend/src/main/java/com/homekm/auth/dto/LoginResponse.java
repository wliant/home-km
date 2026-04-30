package com.homekm.auth.dto;

import java.time.Instant;

/**
 * Login result. Either issues credentials ({@code token}, {@code refreshToken},
 * {@code expiresAt}, {@code user}) or — when the account has MFA enabled —
 * returns {@code mfaRequired=true} with a short-lived {@code mfaChallengeToken}
 * that must be re-submitted to {@code POST /api/auth/mfa/verify-login}.
 */
public record LoginResponse(
        String token,
        String refreshToken,
        Instant expiresAt,
        UserResponse user,
        Boolean mfaRequired,
        String mfaChallengeToken) {

    public LoginResponse(String token, String refreshToken, Instant expiresAt, UserResponse user) {
        this(token, refreshToken, expiresAt, user, null, null);
    }

    public static LoginResponse mfaChallenge(String challengeToken) {
        return new LoginResponse(null, null, null, null, true, challengeToken);
    }
}
