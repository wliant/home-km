# Notification badge count

| Field | Value |
|---|---|
| Category | Functional · PWA & offline |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** No app-icon badge. Unseen reminders, mentions, or comments are invisible until the user opens the app.

**Gap:** No glance-able indicator of pending items.

**Proposed direction:** Use the Badging API (`navigator.setAppBadge(n)`) on supported platforms. Set badge from the service worker on push receipt and from the main thread on app focus (with a count from `GET /api/me/unread`). Clear when the user opens the relevant section.

**References:** `frontend/src/sw.ts`, `frontend/src/components/AppLayout.tsx`, `backend/src/main/java/com/homekm/auth/AuthController.java`, `specs/10-offline-pwa.md`
