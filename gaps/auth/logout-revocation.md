# Logout / session revocation

| Field | Value |
|---|---|
| Category | Functional · Auth & account |
| Priority | P0 |
| Size | M |

**Current state:** Logout clears the token from the frontend `authStore` only. Because JWT is stateless, the issued token remains valid server-side until expiry. There is no way for an admin or user to forcibly revoke a session.

**Gap:** No server-side session revocation. A leaked token is valid for up to 24 hours. Admins cannot kick a compromised account; users cannot "sign out everywhere".

**Proposed direction:** Pair refresh tokens (see `token-refresh.md`) with a server-side `refresh_tokens` table; revoking a row invalidates the session. Add a short-lived JWT denylist (Caffeine cache keyed on `jti`) for emergency revocation of access tokens before expiry. Expose `POST /api/auth/logout` and `POST /api/admin/users/{id}/sessions:revoke`.

**References:** `backend/src/main/java/com/homekm/auth/AuthController.java`, `backend/src/main/java/com/homekm/auth/JwtAuthFilter.java`, `frontend/src/components/AppLayout.tsx`, `specs/02-auth.md`
