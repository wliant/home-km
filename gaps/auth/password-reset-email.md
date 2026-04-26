# Password reset via email

| Field | Value |
|---|---|
| Category | Functional · Auth & account |
| Priority | P0 |
| Size | M |

**Current state:** Users can change their own password via `PATCH /api/auth/me` (when logged in) and admins can reset another user's password via `AdminController`. There is no self-service path for a locked-out user.

**Gap:** A user who forgets their password must beg an admin in person. No SMTP integration, no reset-token flow.

**Proposed direction:** Add SMTP config to `AppProperties` (host, port, user, pass, from). Add `password_reset_tokens` table (token hash, user_id, expires_at, used_at). Endpoints: `POST /api/auth/password-reset/request` (always returns 200 to avoid enumeration) and `POST /api/auth/password-reset/confirm`. Frontend: `/forgot-password` and `/reset-password?token=...` pages. Reuse `LoginRateLimiter` pattern for the request endpoint.

**References:** `backend/src/main/java/com/homekm/auth/AuthController.java`, `backend/src/main/java/com/homekm/admin/AdminController.java`, `backend/src/main/java/com/homekm/common/AppProperties.java`, `specs/02-auth.md`
