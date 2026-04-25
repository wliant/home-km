package com.homekm.reminder.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;

public record ReminderRequest(
        @NotNull @Future Instant remindAt,
        @Pattern(regexp = "daily|weekly|monthly|yearly", message = "invalid recurrence")
        String recurrence,
        List<Long> recipientUserIds
) {}
