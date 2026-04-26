# Multi-recipient UX

| Field | Value |
|---|---|
| Category | Functional · Reminders |
| Priority | P1 |
| Size | S |

**Current state:** `ReminderRecipient` supports many recipients per reminder, but the UI provides only an awkward picker. There is no "everyone in the household" shortcut, no group concept.

**Gap:** Setting a household-wide reminder requires hand-picking each user, repeated for every reminder.

**Proposed direction:** UI v1: an "Everyone" toggle that resolves to all active users. v2: lightweight reminder-recipient groups (`Adults`, `Kids`) created from Settings, addressable in the picker. Backend stays the same — groups expand to recipient rows at save time.

**References:** `backend/src/main/java/com/homekm/reminder/ReminderRecipient.java`, `frontend/src/components/RemindersSection.tsx`, `specs/05-reminders.md`
