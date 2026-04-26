# "Remember me" / extended sessions

| Field | Value |
|---|---|
| Category | Functional · Auth & account |
| Priority | P1 |
| Size | S |

**Current state:** Every login produces an identical 24h token regardless of device trust. Users on a personal phone re-authenticate as often as users on a borrowed laptop.

**Gap:** No mechanism to opt into a longer session on a trusted device, and no way to opt for a stricter session on a shared one.

**Proposed direction:** Add a "Keep me signed in" checkbox on the login form. When checked, the refresh token (see `token-refresh.md`) gets a 30-day lifetime; otherwise 8h. Persist a `device_label` and `last_seen_at` per refresh token so users can review and revoke devices from Settings.

**References:** `frontend/src/features/auth/LoginPage.tsx`, `backend/src/main/java/com/homekm/auth/AuthController.java`, `specs/02-auth.md`
