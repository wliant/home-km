# Calendar (.ics) export / subscription

| Field | Value |
|---|---|
| Category | Functional · Reminders |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** Reminders only fire as Web Push notifications. They are invisible to Apple Calendar, Google Calendar, Thunderbird, etc.

**Gap:** No way to see Home KM reminders alongside the rest of the household calendar.

**Proposed direction:** `GET /api/reminders/me.ics?token=...` returns a per-user `.ics` feed (per-user revocable token, not the JWT). Use `ical4j` to serialize. Each reminder becomes a `VEVENT` with `RRULE` for recurrence. Settings page surfaces the URL with a "Copy" button and a "Regenerate token" action.

**References:** `backend/src/main/java/com/homekm/reminder/ReminderController.java`, `backend/src/main/java/com/homekm/reminder/Reminder.java`, `backend/build.gradle.kts`, `specs/05-reminders.md`
