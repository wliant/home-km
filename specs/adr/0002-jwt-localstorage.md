# ADR-0002: JWT in localStorage instead of HttpOnly cookies

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | @wliant |

## Context

The frontend needs to authenticate API calls. The two common shapes are:
- **Header-based:** JWT in `Authorization: Bearer …`, persisted in `localStorage`.
- **Cookie-based:** session ID or refresh token in an `HttpOnly; Secure; SameSite=Strict` cookie, with a CSRF token shuttled in a header.

Cookie-based is the SPA security canon because XSS cannot read `HttpOnly` cookies. localStorage-based is simpler — no CSRF surface, no cookie middleware, no `withCredentials` plumbing.

For a household-scale self-hosted app the XSS exposure is small: there are no untrusted user inputs rendering as HTML (markdown is sanitised via `rehype-sanitize`), no third-party JS, no advertising. The cookie route's complexity tax is high relative to the marginal XSS risk it removes.

## Decision

The frontend stores the access token (short-lived JWT) and refresh token in `localStorage` via the Zustand `persist` middleware. The API only reads `Authorization: Bearer …`. CSRF protection is disabled because there is no ambient cookie credential a forged cross-site request could ride.

## Consequences

- **Positive:** No CSRF middleware, no cookie domain pinning, no `SameSite` matrix. Service worker reads the token from localStorage when needed (rare — almost all auth happens via the in-page client). API route handlers can be stateless.
- **Negative:** An XSS bug exfiltrates the token. Mitigations: CSP `script-src 'self'` (no inline / no `unsafe-eval`, see `frontend/nginx.conf.template`), rehype-sanitize on every markdown render, no untrusted iframes (`frame-ancestors 'none'`).
- **Neutral:** Refresh tokens still rotate via `JwtAuthFilter` and a denylist on logout (`docs/jwt-rotation.md`).

## Alternatives considered

- **HttpOnly refresh token + in-memory access token.** The "best of both" pattern. Rejected at v1 because the refresh-token-in-cookie path requires CSRF posture work that isn't free, and the SPA's first paint wants to see whether the user is signed in synchronously — an in-memory access token forces a refresh-on-load round trip.
- **Session cookies (Spring Security default).** Rejected — pulls in stateful sessions, breaks horizontal scale-out, and mismatches the stateless API contract.

## References

- `specs/02-auth.md` § 7 (CSRF posture)
- `gaps/auth/token-refresh.md` (closed — see commit history)
- `frontend/src/lib/authStore.ts`
