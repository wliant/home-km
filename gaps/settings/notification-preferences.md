# Per-channel notification preferences

| Field | Value |
|---|---|
| Category | Functional · Settings |
| Priority | P2 |
| Size | S |

**Current state:** Web Push is all-or-nothing — subscribed or unsubscribed. There is no granular control over which event types trigger a push.

**Gap:** A user who only wants reminders gets pushed for every `@mention` once `sharing/comments-mentions.md` lands.

**Proposed direction:** `user_notification_preferences` table or JSONB blob on `users` (`{reminders: ['push','email'], mentions: ['email'], shareLink: []}`). Settings UI: matrix of event type × channel. `PushService` and the new email service consult preferences before dispatching.

**References:** `backend/src/main/java/com/homekm/push/PushService.java`, `backend/src/main/java/com/homekm/auth/User.java`, `frontend/src/features/settings/SettingsPage.tsx`, `specs/05-reminders.md`
