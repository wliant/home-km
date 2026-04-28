# MinIO bucket replication / backup

| Field | Value |
|---|---|
| Category | Non-functional · Backup & DR |
| Priority | P0 |
| Size | S |
| Status | Closed |

**Current state:** MinIO data lives in the `miniodata` Docker volume. No replication, no second-bucket mirror, no off-host backup. File loss == file gone.

**Gap:** No file backup. Loss of the volume loses every uploaded photo, document, and recording.

**Proposed direction:** Use the `mc mirror --watch` command in a small companion container that continuously mirrors the `homekm` bucket to a second target — either a backup MinIO on a different host, an external S3-compatible service, or a local restic repository. Document the recovery path. Versioning (already proposed in `gaps/files/versioning.md`) is complementary but not a substitute.

**References:** `docker-compose.infra.yml`, `backend/src/main/java/com/homekm/file/FileService.java`, `scripts/`, `specs/06-files.md`, `specs/12-infrastructure.md`
