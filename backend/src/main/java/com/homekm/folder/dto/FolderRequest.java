package com.homekm.folder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FolderRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        Long parentId,
        @Size(max = 7) @Pattern(regexp = "^#[0-9a-fA-F]{6}$|^$",
                message = "color must be a #RRGGBB hex string") String color,
        @Size(max = 32) String icon
) {}
