package com.homekm.search.dto;

import java.time.Instant;

public record SearchResult(
        long id,
        String type,
        String title,
        String excerpt,
        Long folderId,
        boolean isChildSafe,
        Instant updatedAt
) {}
