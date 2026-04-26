# OpenTelemetry tracing

| Field | Value |
|---|---|
| Category | Non-functional · Observability |
| Priority | P1 |
| Size | M |

**Current state:** `MdcFilter` populates a `requestId` MDC entry, which is logged in each line. No distributed traces; no spans; no propagation across the API → MinIO or API → DB boundaries.

**Gap:** Hard to debug latency spikes ("the search endpoint is slow — is it the DB, MinIO, or our code?").

**Proposed direction:** Add OpenTelemetry Java agent (`-javaagent:opentelemetry-javaagent.jar`) — auto-instruments Spring Web, JDBC, HikariCP, and the MinIO client. Configure OTLP export to a Tempo or Jaeger container in `docker-compose.observability.yml`. Frontend: `@opentelemetry/sdk-trace-web` if/when worth the bundle cost. Reuse existing `requestId` as `trace-id` to bridge old and new logs.

**References:** `backend/src/main/java/com/homekm/common/MdcFilter.java`, `backend/Dockerfile`, `docker-compose.app.yml`, `specs/12-infrastructure.md`
