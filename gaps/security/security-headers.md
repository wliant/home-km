# Security headers (CSP, HSTS, X-Frame-Options, X-Content-Type-Options)

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P0 |
| Size | S |

**Current state:** `frontend/nginx.conf.template` sets only `Cache-Control` headers. No `Content-Security-Policy`, no `Strict-Transport-Security`, no `X-Frame-Options`, no `X-Content-Type-Options`, no `Referrer-Policy`. `grep -ri "Content-Security-Policy" frontend backend` returns nothing.

**Gap:** No defense-in-depth against XSS, clickjacking, MIME sniffing, or referrer leakage.

**Proposed direction:** Add to `nginx.conf.template`: a strict CSP (`default-src 'self'; img-src 'self' blob: data:; connect-src 'self'`); `Strict-Transport-Security: max-age=31536000; includeSubDomains` (once TLS is in place); `X-Frame-Options: DENY`; `X-Content-Type-Options: nosniff`; `Referrer-Policy: strict-origin-when-cross-origin`; `Permissions-Policy: geolocation=(), camera=(self), microphone=(self)`. Verify with `securityheaders.com` after deploy.

**References:** `frontend/nginx.conf.template`, `frontend/Dockerfile`, `specs/12-infrastructure.md`
