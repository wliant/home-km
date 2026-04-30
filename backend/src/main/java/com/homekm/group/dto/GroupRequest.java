package com.homekm.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GroupRequest(
        @NotBlank @Size(max = 80) String name,
        List<Long> memberUserIds
) {}
