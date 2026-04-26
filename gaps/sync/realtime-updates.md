# Real-time updates (WebSocket / SSE)

| Field | Value |
|---|---|
| Category | Functional · Multi-device sync |
| Priority | P1 |
| Size | M |

**Current state:** All updates are pull-based via TanStack Query. A user adding a note on phone will not see it on a second open device until that device refetches (typically on tab focus).

**Gap:** No live propagation of changes across devices and tabs. Real-time use cases (`sharing/shared-shopping-lists.md`) are blocked.

**Proposed direction:** Add a Server-Sent Events endpoint `GET /api/events` (lighter than WebSockets, no extra infra). Server publishes per-household events (item_changed, comment_added, reminder_fired) keyed by user. Frontend hook `useLiveUpdates()` invalidates the relevant TanStack Query keys on receipt — UI just rerenders normally. Reuse `MdcFilter` for request tracing.

**References:** `backend/src/main/java/com/homekm/common/`, `backend/src/main/java/com/homekm/auth/JwtAuthFilter.java`, `frontend/src/lib/queryKeys.ts`, `frontend/src/api/`, `specs/14-frontend-architecture.md`
