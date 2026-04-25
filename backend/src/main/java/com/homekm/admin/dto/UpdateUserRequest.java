package com.homekm.admin.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1, max = 100)
        @Pattern(regexp = "^[^\\p{Cntrl}]+$", message = "must not contain control characters")
        String displayName,
        Boolean isAdmin,
        Boolean isChild,
        Boolean isActive
) {}
