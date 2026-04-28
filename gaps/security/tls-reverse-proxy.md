# TLS termination + reverse proxy

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P0 |
| Size | S |
| Status | Closed |

**Current state:** `frontend/Dockerfile` ships an `nginx:alpine` listening on port 80 only. `frontend/nginx.conf.template` configures `listen 80;`. There is no TLS, no certificate, no production reverse proxy in front of the stack.

**Gap:** Credentials, JWTs, and uploaded files travel in plaintext over the LAN — and over the internet if the user exposes the box. Trivially intercepted on an untrusted WiFi.

**Proposed direction:** Add an optional Caddy (or Traefik) container to `docker-compose.app.yml` configured to obtain a Let's Encrypt cert for the user-supplied hostname (with internal CA fallback for LAN-only deployments). Caddy proxies `/api/` to the API container and `/` to the frontend nginx. Document HTTP→HTTPS redirect and HSTS in the runbook (`non-functional/documentation/operator-runbook.md`).

**References:** `frontend/nginx.conf.template`, `frontend/Dockerfile`, `docker-compose.app.yml`, `specs/12-infrastructure.md`
