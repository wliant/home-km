package com.homekm.file.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FileUpdateRequest(
        @Size(max = 500)
        @Pattern(regexp = "^(?!\\s*$).+", message = "must not be blank")
        String filename,
        String description,
        Long folderId,
        Boolean isChildSafe
) {}
