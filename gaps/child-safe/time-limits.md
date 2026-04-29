# Time limits / quiet hours

| Field | Value |
|---|---|
| Category | Functional · Child-safe mode |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** A child account can sign in and use the app any time of day or night.

**Gap:** No "no screens after 9pm" enforcement at the app level.

**Proposed direction:** Per-user `quiet_hours_start TIME`, `quiet_hours_end TIME`, `timezone` (also useful for `reminders/timezone-handling.md`). During quiet hours, child accounts get a soft block screen with the time they can return. Admin override is always available.

**References:** `backend/src/main/java/com/homekm/auth/User.java`, `backend/src/main/java/com/homekm/auth/JwtAuthFilter.java`, `frontend/src/components/ProtectedRoute.tsx`, `specs/09-child-safe.md`
