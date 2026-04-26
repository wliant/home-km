# Account deletion + GDPR-style data export

| Field | Value |
|---|---|
| Category | Non-functional · Compliance & data |
| Priority | P2 |
| Size | M |

**Current state:** Self-service deletion and data export are missing entirely (see functional `settings/account-deletion.md` and `settings/gdpr-export.md`). Even though Home KM is single-tenant household-scope, several jurisdictions require these capabilities for any system holding personal data.

**Gap:** Compliance gap if the instance ever stores data of users beyond the immediate household (extended family, guests).

**Proposed direction:** Treat the two functional gaps as the compliance requirement. Document the documented retention/deletion path in a `PRIVACY.md`. Map to GDPR articles 17 (erasure) and 20 (portability) for operators in regulated jurisdictions. Default position: features available but disabled by default to keep the simple-household setup unchanged.

**References:** `backend/src/main/java/com/homekm/auth/AuthController.java`, `backend/src/main/java/com/homekm/admin/AdminService.java`, `specs/02-auth.md`
