# OIDC / SSO consideration

| Field | Value |
|---|---|
| Category | Functional · Auth & account |
| Priority | P2 |
| Size | L |

**Current state:** Local-only email/password. No SSO.

**Gap:** Households running a self-hosted identity provider (Authelia, Keycloak, Authentik) cannot reuse it for Home KM. Each user maintains a separate password.

**Proposed direction:** Optional OIDC integration via Spring Security's OAuth2 client. Configurable per-environment (`app.oidc.issuer`, `client-id`, etc.) and disabled by default. Map OIDC `sub` to a `user_oidc_identity` table and provision local users on first login (admin can pre-allow specific email domains). Keep local password auth as an always-available fallback.

**References:** `backend/src/main/java/com/homekm/auth/`, `backend/build.gradle.kts`, `specs/02-auth.md`
