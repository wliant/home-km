# Restore guide

| Field | Value |
|---|---|
| Category | Non-functional · Documentation |
| Priority | P0 |
| Size | S |
| Status | Closed |

**Current state:** Once backups exist (`backup-dr/postgres-backups.md`, `backup-dr/minio-backups.md`), the restore path is undocumented.

**Gap:** Untested, undocumented restore is no restore.

**Proposed direction:** A `RESTORE.md` runbook with concrete commands for: Postgres restore from `pg_dump`, Postgres restore from pgbackrest, MinIO restore from `mc mirror` target, MinIO restore from restic snapshot. Include "test on a throwaway host first" warning. Cross-link from the operator runbook (`operability/runbook.md`) and the monthly restore drill (`backup-dr/restore-drill.md`).

**References:** `scripts/`, `docker-compose.infra.yml`, `specs/12-infrastructure.md`
