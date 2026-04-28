# Invite-only registration flow

| Field | Value |
|---|---|
| Category | Functional · Admin |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** `POST /api/auth/register` is open. Anyone who can reach the API URL can create an account. The host can disable registration only by removing the route or via a network firewall.

**Gap:** No way to keep the open-registration convenience for new family members while blocking strangers if the instance is exposed to the internet.

**Proposed direction:** Add `app.registration.mode` (open / invite / closed; default `invite`). In `invite` mode, registration requires a valid `invite_token` (admin-issued, single-use, 7-day expiry). Admin UI: "Generate invite link". Pairs with `admin/bulk-user-import.md` for the same token primitive.

**References:** `backend/src/main/java/com/homekm/auth/AuthController.java`, `backend/src/main/java/com/homekm/common/AppProperties.java`, `frontend/src/features/auth/RegisterPage.tsx`, `specs/02-auth.md`
