# Multi-factor authentication (TOTP)

| Field | Value |
|---|---|
| Category | Functional · Auth & account |
| Priority | P2 |
| Size | M |

**Current state:** Single-factor email/password. Login is rate-limited (20/min/IP) but no second factor.

**Gap:** No MFA. For a household admin account that can read every member's notes and files, single-factor auth is weak — especially over a LAN exposed to guests.

**Proposed direction:** TOTP (RFC 6238) using a Java library such as `dev.samstevens.totp`. Tables: `user_mfa_secrets`, `user_mfa_recovery_codes` (hashed). Endpoints: enroll, verify-and-enable, disable, recover. Login flow returns `MFA_REQUIRED` with a short-lived `mfa_challenge_token` instead of a JWT until the second factor is verified. Frontend: enrollment screen with QR code (use `qrcode` npm package).

**References:** `backend/src/main/java/com/homekm/auth/AuthController.java`, `backend/src/main/java/com/homekm/auth/User.java`, `frontend/src/features/settings/SettingsPage.tsx`, `specs/02-auth.md`
