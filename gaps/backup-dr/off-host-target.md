# Off-host backup target

| Field | Value |
|---|---|
| Category | Non-functional · Backup & DR |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** Even when backups exist (`postgres-backups.md`, `minio-backups.md`), they sit on the same host. A house fire takes the originals and the backups together.

**Gap:** Single-location failure has no recovery.

**Proposed direction:** Recommend a paid or self-hosted off-host target — Backblaze B2, hetzner storage box, or a friend's server running restic. Provide a sample `restic` config in `scripts/` that pushes the local backup directory once per night, encrypted. Keep three retention tiers: 7 daily, 4 weekly, 12 monthly. Document trade-offs (recurring cost vs trust vs convenience) in the operator runbook.

**References:** `scripts/`, `specs/12-infrastructure.md`
