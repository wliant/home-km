# PostgreSQL automated backups + WAL archiving

| Field | Value |
|---|---|
| Category | Non-functional · Backup & DR |
| Priority | P0 |
| Size | M |
| Status | Closed |

**Current state:** PostgreSQL data lives in the named Docker volume `pgdata`. There are no backup scripts, no WAL archiving, no off-host copy. A failed disk equals total data loss.

**Gap:** No backups. Catastrophic data-loss risk for the household's primary memory store.

**Proposed direction:** Add a `pgbackrest` (or simpler `pg_dump` cron) sidecar container to the infra compose file. Daily full + hourly incremental dumps to a host-mounted directory `./backups/postgres/`. Encrypt at rest using a configurable passphrase. Provide a `scripts/restore-postgres.sh` for the inverse path. Document in operator runbook (`documentation/operator-runbook.md`) that the backups directory must be replicated off-host (rsync, restic, Borg).

**References:** `docker-compose.infra.yml`, `infra/postgres/`, `scripts/`, `specs/12-infrastructure.md`
