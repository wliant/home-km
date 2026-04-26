# Response compression (gzip / brotli) and HTTP/2

| Field | Value |
|---|---|
| Category | Non-functional · Performance |
| Priority | P2 |
| Size | S |

**Current state:** `frontend/nginx.conf.template` does not enable `gzip` or `brotli`. The static bundle and JSON API responses are sent uncompressed. nginx serves HTTP/1.1.

**Gap:** Wasted bandwidth on every page load and every list response. No request multiplexing.

**Proposed direction:** Enable nginx `gzip on;` + `gzip_types text/css application/javascript application/json image/svg+xml;` and `brotli on;` (where the brotli module is available). Once TLS lands (`security/tls-reverse-proxy.md`), the Caddy/Traefik front gets HTTP/2 for free. Backend can additionally enable `server.compression.enabled=true` so direct API hits also compress.

**References:** `frontend/nginx.conf.template`, `backend/src/main/resources/application.yml`, `specs/12-infrastructure.md`
