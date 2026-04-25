package com.homekm.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank @Size(max = 500) String title,
        @Size(max = 100_000) String body,
        @Pattern(regexp = "recipe|todo|reminder|shopping_list|home_items|usage_manual|goal|aspiration|wish_list|travel_log|custom",
                 message = "invalid label") String label,
        Long folderId,
        Boolean isChildSafe
) {}
