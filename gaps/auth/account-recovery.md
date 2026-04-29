# Account recovery

| Field | Value |
|---|---|
| Category | Functional · Auth & account |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** Only an admin can restore access to a locked-out user. No recovery codes, no recovery email, no security questions.

**Gap:** If the sole admin loses their credentials, the household has no path back into the system short of dropping the database.

**Proposed direction:** (1) MFA recovery codes (covered in `mfa-totp.md`). (2) A documented "break-glass" admin reset script in `scripts/` that an operator can run on the host to set a new password directly in PostgreSQL. (3) Optional: a backup-admin pattern requiring two admins — first admin can be reset by a second admin only.

**References:** `backend/src/main/java/com/homekm/admin/AdminService.java`, `scripts/`, `specs/02-auth.md`
