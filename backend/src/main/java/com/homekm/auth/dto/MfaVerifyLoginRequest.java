package com.homekm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MfaVerifyLoginRequest(
        @NotBlank String challengeToken,
        @NotBlank String code,
        Boolean rememberMe,
        @Size(max = 120) String deviceLabel
) {}
