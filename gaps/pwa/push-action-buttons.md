# Push notification action buttons

| Field | Value |
|---|---|
| Category | Functional · PWA & offline |
| Priority | P2 |
| Size | S |

**Current state:** Push notifications fired by `ReminderScheduler` show only a title and body. Tapping opens the related note.

**Gap:** No inline actions. A reminder for "take out trash" cannot be dismissed or snoozed without opening the app.

**Proposed direction:** Include `actions: [{action: 'done', title: 'Done'}, {action: 'snooze-1h', title: 'Snooze 1h'}]` in the push payload. Service worker handles `notificationclick` with `event.action`. POSTs to `/api/reminders/{id}/done` or the snooze endpoint from `reminders/snooze.md`. Note: action button support varies (Chromium yes, Safari no — degrade gracefully).

**References:** `frontend/src/sw.ts`, `backend/src/main/java/com/homekm/push/PushService.java`, `backend/src/main/java/com/homekm/reminder/ReminderScheduler.java`, `specs/05-reminders.md`, `specs/10-offline-pwa.md`
