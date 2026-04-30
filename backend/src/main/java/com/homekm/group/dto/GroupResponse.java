package com.homekm.group.dto;

import java.time.Instant;
import java.util.List;

public record GroupResponse(
        Long id,
        String name,
        String kind,
        boolean isSystem,
        List<Long> memberUserIds,
        Instant createdAt
) {}
