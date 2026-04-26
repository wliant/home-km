# SLO definition

| Field | Value |
|---|---|
| Category | Non-functional · Reliability |
| Priority | P2 |
| Size | S |

**Current state:** No defined Service Level Objectives. Acceptable error rate, latency p99, and uptime are implicit.

**Gap:** No shared definition of "is the system healthy?". Operators cannot tell when a regression is bad enough to roll back.

**Proposed direction:** Document SLOs sized for a household app: API availability 99.5% monthly, p95 latency < 500ms for read endpoints and < 2s for upload, push delivery success 95%. Tie to Prometheus alerts (`observability/metrics-prometheus.md`). Revisit annually based on actual traffic.

**References:** `specs/00-overview.md`, `specs/12-infrastructure.md`, `backend/src/main/resources/application.yml`
