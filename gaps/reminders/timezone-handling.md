# Timezone awareness

| Field | Value |
|---|---|
| Category | Functional · Reminders |
| Priority | P1 |
| Size | S |

**Current state:** All timestamps stored in UTC; the Jackson config writes dates with context timezone. The UI assumes the user is in a single timezone, but `ReminderScheduler` fires based on UTC clock without per-user TZ context.

**Gap:** A household member traveling abroad gets reminders at 3am local time. No way to set a per-user timezone.

**Proposed direction:** Add `timezone VARCHAR(64)` (IANA zone) on `users`, defaulting to the server's local zone. Frontend captures `Intl.DateTimeFormat().resolvedOptions().timeZone` on login. `ReminderScheduler` resolves recipient timezones when computing the next fire time for recurring reminders.

**References:** `backend/src/main/java/com/homekm/reminder/ReminderScheduler.java`, `backend/src/main/java/com/homekm/auth/User.java`, `backend/src/main/resources/application.yml`, `frontend/src/lib/authStore.ts`, `specs/05-reminders.md`
