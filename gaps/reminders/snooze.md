# Reminder snooze

| Field | Value |
|---|---|
| Category | Functional · Reminders |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** When `ReminderScheduler` fires a reminder, the user receives one push and that's it — it's gone. To be reminded again later, the user must edit the reminder.

**Gap:** No "remind me again in 1h / tonight / tomorrow" action — universally expected from any reminder app.

**Proposed direction:** Add quick-snooze action buttons to the Web Push notification (`actions: [{action: 'snooze-1h'}, {action: 'snooze-tomorrow'}]`). Service worker handles the click and POSTs to `POST /api/reminders/{id}/snooze` with a duration. Server adjusts `remind_at` and resets the "delivered" marker so it fires again.

**References:** `backend/src/main/java/com/homekm/reminder/ReminderScheduler.java`, `backend/src/main/java/com/homekm/reminder/Reminder.java`, `frontend/src/sw.ts`, `frontend/src/components/RemindersSection.tsx`, `specs/05-reminders.md`
