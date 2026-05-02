package com.homekm.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Decides whether a notification should be skipped because the recipient is
 * inside their configured quiet-hours window. Times are stored as wall-clock
 * {@link LocalTime} on the user row (V021); we evaluate them in the user's
 * configured timezone (V014's {@code users.timezone}).
 *
 * Wrapping windows are supported — a window like 21:00–07:00 means quiet
 * after 9pm and before 7am, including across midnight. A null start or end
 * disables the check entirely (default for fresh accounts).
 */
@Service
public class QuietHoursService {

    public boolean isQuiet(User user, Instant now) {
        LocalTime start = user.getQuietHoursStart();
        LocalTime end = user.getQuietHoursEnd();
        if (start == null || end == null) return false;
        if (start.equals(end)) return false; // no real window

        ZoneId zone;
        try {
            zone = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "UTC");
        } catch (Exception e) {
            zone = ZoneId.of("UTC");
        }
        LocalTime local = now.atZone(zone).toLocalTime();

        if (start.isBefore(end)) {
            // Same-day window: quiet if [start, end).
            return !local.isBefore(start) && local.isBefore(end);
        }
        // Wrapping window (e.g. 21:00–07:00): quiet if >= start OR < end.
        return !local.isBefore(start) || local.isBefore(end);
    }
}
