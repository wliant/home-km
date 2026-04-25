package com.homekm.file.dto;

import jakarta.validation.constraints.Size;

public record FileUpdateRequest(
        @Size(max = 500) String filename,
        String description,
        Long folderId,
        Boolean isChildSafe
) {}
