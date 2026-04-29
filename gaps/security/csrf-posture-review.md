# CSRF posture review

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** Spring Security has CSRF disabled (correct for a stateless JWT-in-Authorization-header API). No cookie-based session, so no traditional CSRF surface.

**Gap:** Posture is currently safe but undocumented. If a future change introduces cookies (e.g., refresh-token httpOnly cookie from `auth/token-refresh.md`), the implicit assumption breaks.

**Proposed direction:** Document the CSRF posture in `specs/02-auth.md`. When refresh tokens land, set the cookie `SameSite=Strict; HttpOnly; Secure` and re-evaluate whether the refresh endpoint needs CSRF protection (most implementations require a header-confirmed token in addition to the cookie).

**References:** `backend/src/main/java/com/homekm/auth/SecurityConfig.java`, `backend/src/main/java/com/homekm/auth/JwtAuthFilter.java`, `specs/02-auth.md`
