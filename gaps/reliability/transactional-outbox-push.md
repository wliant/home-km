# Transactional outbox for push notifications

| Field | Value |
|---|---|
| Category | Non-functional · Reliability |
| Priority | P2 |
| Size | M |

**Current state:** `ReminderScheduler` reads due reminders, calls `PushService.send(...)` synchronously inside the same loop, and (presumably) marks the reminder delivered. If push delivery succeeds but the DB update fails, the reminder fires again on the next tick. If the DB succeeds and push fails, the user never gets it.

**Gap:** Dual-write between database and external push service has no atomicity.

**Proposed direction:** Add an `outbox_events` table. The reminder loop writes "to be pushed" rows in the same transaction as marking the reminder. A separate publisher polls the outbox, calls Web Push, and removes rows on success (or retries with backoff on failure). Pattern is well-known; covers comments/mentions and any future event-driven feature.

**References:** `backend/src/main/java/com/homekm/reminder/ReminderScheduler.java`, `backend/src/main/java/com/homekm/push/PushService.java`, `specs/05-reminders.md`
