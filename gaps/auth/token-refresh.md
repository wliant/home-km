# Token refresh

| Field | Value |
|---|---|
| Category | Functional · Auth & account |
| Priority | P0 |
| Size | M |
| Status | Closed |

**Current state:** Single JWT issued at login with a 24h expiry (`JWT_EXPIRY_HOURS=24`). When the token expires, the user is silently logged out mid-session. `authStore` only checks expiry on rehydrate, not on each request, so 401s appear inconsistently.

**Gap:** No refresh-token mechanism. No way to extend a session without re-prompting for credentials. Long-lived 24h tokens trade UX for a wider blast radius if a token leaks.

**Proposed direction:** Issue a short-lived access token (~15 min) plus a longer-lived refresh token (7–30 days) stored httpOnly. Add `POST /api/auth/refresh` that rotates the refresh token and returns a new access token. Wire a TanStack Query mutation/axios interceptor on the frontend to refresh transparently on 401.

**References:** `backend/src/main/java/com/homekm/auth/JwtService.java`, `backend/src/main/java/com/homekm/auth/AuthController.java`, `frontend/src/lib/authStore.ts`, `specs/02-auth.md`
