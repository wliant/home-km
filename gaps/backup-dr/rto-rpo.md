# Documented RTO / RPO

| Field | Value |
|---|---|
| Category | Non-functional · Backup & DR |
| Priority | P2 |
| Size | S |

**Current state:** No defined recovery objectives.

**Gap:** Operator has no target. "Did we restore fast enough?" is unanswerable.

**Proposed direction:** Sized for a household: RTO 4h (acceptable downtime), RPO 1h (acceptable data loss). Documented in `specs/00-overview.md` as a non-goal-bounding constraint. Backup cadence (`postgres-backups.md`, `minio-backups.md`) and restore drill design must satisfy these targets.

**References:** `specs/00-overview.md`, `specs/12-infrastructure.md`
