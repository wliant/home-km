package com.homekm.note.dto;

import jakarta.validation.constraints.NotBlank;

public record ChecklistItemRequest(
        @NotBlank String text,
        Boolean isChecked,
        Integer sortOrder
) {}
