# Log aggregation (Loki / ELK)

| Field | Value |
|---|---|
| Category | Non-functional · Observability |
| Priority | P1 |
| Size | S |

**Current state:** Logs only exist as the container's stdout, accessible via `docker compose logs`. No retention beyond Docker's default rotation; no full-text search; cannot correlate across services.

**Gap:** Triaging a problem requires `grep`-ing through `docker logs api`.

**Proposed direction:** Recommend Loki as the lightweight default — single binary, single Docker container, queryable via Grafana (`grafana-dashboards.md`). Provision a Promtail sidecar (or Docker logging driver) to ship logs from `api` and `frontend` containers. Default retention 30 days. Document in operator runbook. ELK left as an alternative for users already running it.

**References:** `docker-compose.app.yml`, `docker-compose.infra.yml`, `infra/`, `specs/12-infrastructure.md`
