package com.homekm.push.dto;

import jakarta.validation.constraints.NotBlank;

public record PushSubscribeRequest(
        @NotBlank String endpoint,
        @NotBlank String p256dhKey,
        @NotBlank String authKey,
        String userAgent
) {}
