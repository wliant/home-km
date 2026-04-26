# Cron-style custom recurrence

| Field | Value |
|---|---|
| Category | Functional · Reminders |
| Priority | P2 |
| Size | M |

**Current state:** `Reminder.recurrence` is constrained to `daily`, `weekly`, `monthly`, `yearly` (validated by a CHECK constraint).

**Gap:** No "every weekday", "every other Tuesday", "first of every month", "last weekday of the month" — common household patterns.

**Proposed direction:** Replace the enum with iCalendar `RRULE` strings (well-defined, library-supported by `ical4j`, also covers `ics-export.md`). Provide a UI builder that constructs RRULEs from common presets plus a free-form expert mode. Migrate existing rows in a Flyway script (`daily` → `FREQ=DAILY`, etc.).

**References:** `backend/src/main/java/com/homekm/reminder/Reminder.java`, `backend/src/main/java/com/homekm/reminder/ReminderScheduler.java`, `backend/src/main/resources/db/migration/V001__init.sql`, `specs/05-reminders.md`
