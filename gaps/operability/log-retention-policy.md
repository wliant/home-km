# Log retention policy

| Field | Value |
|---|---|
| Category | Non-functional · Operability |
| Priority | P2 |
| Size | S |

**Current state:** Container logs accumulate on the Docker host with default driver settings (json-file, no rotation cap unless configured globally on the daemon).

**Gap:** Disk fills silently from log growth.

**Proposed direction:** Configure per-service `logging:` blocks in compose: `driver: json-file`, `options: {max-size: '50m', max-file: '5'}`. When `observability/log-aggregation.md` lands, retention shifts to Loki with a 30-day default. Document in operator runbook.

**References:** `docker-compose.app.yml`, `docker-compose.infra.yml`, `specs/12-infrastructure.md`
