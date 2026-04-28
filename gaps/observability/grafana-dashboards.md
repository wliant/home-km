# Grafana dashboards

| Field | Value |
|---|---|
| Category | Non-functional · Observability |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** No dashboards. Once metrics exist (see `metrics-prometheus.md`), they would be raw without visualization.

**Gap:** No visual at-a-glance view of system health.

**Proposed direction:** Add Grafana to the optional `docker-compose.observability.yml`. Provision two dashboards as JSON in `infra/grafana/`: (1) "Home KM — system" (JVM, HTTP rate/latency/errors, HikariCP, MinIO request counts), (2) "Home KM — product" (active users, items per type, storage growth, push delivery rate). Provisioning auto-loaded via Grafana's file-based provisioning.

**References:** `docker-compose.app.yml`, `infra/`, `specs/12-infrastructure.md`
