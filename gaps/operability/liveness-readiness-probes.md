# Separate liveness vs readiness probes

| Field | Value |
|---|---|
| Category | Non-functional · Operability |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** `docker-compose.app.yml` healthcheck uses `wget /actuator/health` for both readiness and liveness. The endpoint always returns `UP` once Spring boots, regardless of dependency state.

**Gap:** No way to distinguish "Spring is alive but DB is broken" (don't restart, don't route traffic) from "Spring is hung" (restart).

**Proposed direction:** Expose `/actuator/health/liveness` (true if JVM alive) and `/actuator/health/readiness` (true only if DB + MinIO are reachable). Spring Boot supports this natively — enable `management.endpoint.health.probes.enabled=true`. Compose healthcheck switches to readiness for traffic gating; an additional Docker `start_period` covers initial boot.

**References:** `backend/src/main/resources/application.yml`, `docker-compose.app.yml`, `specs/12-infrastructure.md`
