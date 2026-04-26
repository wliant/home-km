# Bulk user import

| Field | Value |
|---|---|
| Category | Functional · Admin |
| Priority | P2 |
| Size | S |

**Current state:** Admin must create users one by one in `AdminController`.

**Gap:** Onboarding a 6-person household at first deployment requires 6 round trips through the form.

**Proposed direction:** `POST /api/admin/users/bulk` accepting CSV (email, displayName, isAdmin, isChild). Generates a one-time setup link per user (token-based, 7-day expiry) so admins don't need to know other users' passwords. Setup page lets the user choose their password and complete profile. Reuses the password-reset token plumbing from `auth/password-reset-email.md`.

**References:** `backend/src/main/java/com/homekm/admin/AdminController.java`, `backend/src/main/java/com/homekm/admin/AdminService.java`, `frontend/src/features/admin/`, `specs/02-auth.md`
