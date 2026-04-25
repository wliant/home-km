package com.homekm.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8) @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "must contain at least one uppercase letter, one lowercase letter, and one digit"
        ) String password,
        @NotBlank @Size(min = 1, max = 100)
        @Pattern(regexp = "^[^\\p{Cntrl}]+$", message = "must not contain control characters")
        String displayName
) {}
