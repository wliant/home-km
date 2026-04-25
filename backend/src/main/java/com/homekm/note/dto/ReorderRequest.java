package com.homekm.note.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderRequest(@NotNull @Valid List<ReorderItem> items) {
    public record ReorderItem(@NotNull Long id, @NotNull Integer sortOrder) {}
}
