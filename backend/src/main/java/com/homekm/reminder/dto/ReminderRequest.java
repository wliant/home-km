package com.homekm.reminder.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record ReminderRequest(
        @NotNull @Future Instant remindAt,
        /**
         * iCalendar RRULE string (e.g. {@code FREQ=WEEKLY;BYDAY=MO,WE,FR}).
         * Legacy enum values (daily/weekly/monthly/yearly) still parse for
         * backwards compatibility with older clients, but new clients
         * should send full RRULEs. ical4j validates semantics at advance
         * time inside {@code ReminderScheduler.advanceRemindAt}.
         */
        @Pattern(regexp = "^(daily|weekly|monthly|yearly|FREQ=[A-Z;,=0-9+\\-]+)$",
                 message = "invalid recurrence; use an iCalendar RRULE")
        @Size(max = 255)
        String recurrence,
        List<Long> recipientUserIds,
        /**
         * Group IDs whose members should also receive the reminder. Expanded
         * to user IDs at create/update time and merged with {@code recipientUserIds}.
         */
        List<Long> recipientGroupIds
) {
    public ReminderRequest(Instant remindAt, String recurrence, List<Long> recipientUserIds) {
        this(remindAt, recurrence, recipientUserIds, null);
    }
}
