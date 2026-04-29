# Account deletion (self-service)

| Field | Value |
|---|---|
| Category | Functional · Settings |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** Only an admin can delete a user via `AdminController`. A user cannot request their own deletion.

**Gap:** No self-service account deletion.

**Proposed direction:** Settings → Danger Zone → "Delete my account". Requires password confirmation and a 7-day soft-delete window during which sign-in is blocked but data is preserved (and `data-export` can run if requested first). On day 8, hard-delete: cascade through items the user owns according to a documented policy (re-assign household-shared items to the primary admin; hard-delete personal items if `sharing/per-item-acls.md` exists).

**References:** `backend/src/main/java/com/homekm/auth/AuthController.java`, `backend/src/main/java/com/homekm/admin/AdminService.java`, `frontend/src/features/settings/SettingsPage.tsx`, `specs/02-auth.md`
