package com.homekm.auth.dto;

import java.time.Instant;

public record LoginResponse(String token, String refreshToken, Instant expiresAt, UserResponse user) {}
