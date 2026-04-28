# Metrics (Micrometer + Prometheus)

| Field | Value |
|---|---|
| Category | Non-functional · Observability |
| Priority | P0 |
| Size | S |
| Status | Closed |

**Current state:** `application.yml` exposes only the `health` actuator endpoint (`management.endpoints.web.exposure.include: health`). No metrics, no JVM, no HikariCP, no HTTP request stats.

**Gap:** No production telemetry. Cannot answer "is the API slow?" or "are we leaking connections?".

**Proposed direction:** Add `spring-boot-starter-actuator`'s Micrometer Prometheus registry. Expose `prometheus` and `info` endpoints, secured behind a separate management port or basic auth. Add a Prometheus container to a new `docker-compose.observability.yml` (opt-in). Custom counters for: reminders fired, push deliveries (success/fail), file uploads, search queries.

**References:** `backend/src/main/resources/application.yml`, `backend/build.gradle.kts`, `docker-compose.app.yml`, `specs/12-infrastructure.md`
