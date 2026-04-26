# Dependency-aware health (DB + MinIO)

| Field | Value |
|---|---|
| Category | Non-functional · Operability |
| Priority | P1 |
| Size | S |

**Current state:** The actuator health indicator only reflects Spring's lifecycle. `application.yml` sets `endpoint.health.show-details: never` — even if indicators existed, they'd be hidden.

**Gap:** No external visibility into whether MinIO or PostgreSQL is reachable from the app.

**Proposed direction:** Add a custom `MinioHealthIndicator` that does a cheap `bucketExists(...)` call. PostgreSQL has a built-in `DataSourceHealthIndicator`. Expose to the readiness probe (`liveness-readiness-probes.md`). Set `show-details: when-authorized` so authenticated admin users can see the detail; anonymous callers still get a binary up/down.

**References:** `backend/src/main/java/com/homekm/file/FileService.java`, `backend/src/main/resources/application.yml`, `backend/src/main/java/com/homekm/common/`, `specs/12-infrastructure.md`
