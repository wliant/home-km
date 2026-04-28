package com.homekm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password,
        Boolean rememberMe,
        @Size(max = 120) String deviceLabel
) {
    public LoginRequest(String email, String password) {
        this(email, password, null, null);
    }
}
