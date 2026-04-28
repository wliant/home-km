# Slow-query logging

| Field | Value |
|---|---|
| Category | Non-functional · Performance |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** No slow-query logging configured in PostgreSQL or Hibernate. Slow queries (e.g., a search hitting an unindexed predicate) are invisible until users complain.

**Gap:** No data to drive index decisions.

**Proposed direction:** Set `log_min_duration_statement = 500ms` in `infra/postgres/init.sql` (or via a runtime override). Enable Hibernate statistics in dev profile only. Optionally add `pg_stat_statements` extension for aggregate slow-query analysis. Surface results in Grafana via `postgres_exporter` once observability is in place.

**References:** `infra/postgres/init.sql`, `backend/src/main/resources/application.yml`, `docker-compose.infra.yml`, `specs/12-infrastructure.md`
