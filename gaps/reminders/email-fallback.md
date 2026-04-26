# Email fallback when push fails

| Field | Value |
|---|---|
| Category | Functional · Reminders |
| Priority | P2 |
| Size | S |

**Current state:** A reminder fires via Web Push only. If the recipient has no `PushSubscription` (e.g., never enabled push, or the subscription expired), the reminder is silently dropped.

**Gap:** No fallback channel. Users on iOS Safari, where Web Push is restricted to installed PWAs, miss reminders entirely.

**Proposed direction:** Once SMTP is wired (`auth/password-reset-email.md`), send an email if no push subscription succeeds (or all 410-Gone). Add per-user channel preferences in Settings: push only, email only, both. Track delivery in a `reminder_deliveries` table (channel, status, error).

**References:** `backend/src/main/java/com/homekm/reminder/ReminderScheduler.java`, `backend/src/main/java/com/homekm/push/PushService.java`, `backend/src/main/java/com/homekm/common/AppProperties.java`, `specs/05-reminders.md`
