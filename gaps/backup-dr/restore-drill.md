# Monthly restore drill

| Field | Value |
|---|---|
| Category | Non-functional · Backup & DR |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** Even when backups exist, no documented restore procedure has been tested.

**Gap:** Backups that haven't been restored aren't backups.

**Proposed direction:** Document a step-by-step restore using a fresh Docker host into a `RESTORE.md` runbook section. Schedule a calendar reminder (use Home KM itself!) for a monthly restore drill into a temporary stack (`docker-compose.restore-test.yml`). Verify the restored instance can serve `/actuator/health` and one canary note. Keep results log.

**References:** `docker-compose.infra.yml`, `docker-compose.app.yml`, `scripts/`, `specs/12-infrastructure.md`
