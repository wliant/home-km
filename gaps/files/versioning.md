# File versioning

| Field | Value |
|---|---|
| Category | Functional · Files |
| Priority | P2 |
| Size | M |

**Current state:** `PUT /api/files/{id}/content` (replace endpoint) overwrites the MinIO object in place. The old bytes are lost.

**Gap:** A re-uploaded file destroys the previous version. No way to roll back to last week's spreadsheet.

**Proposed direction:** Enable MinIO bucket versioning on `homekm` and store the version ID in a new `stored_file_versions` table per upload (size, mime, uploaded_by, uploaded_at, minio_version_id). Detail page lists versions with download and "Make current" actions. Cap retained versions per file (e.g., last 10 + last 90 days).

**References:** `backend/src/main/java/com/homekm/file/FileService.java`, `infra/postgres/init.sql`, `docker-compose.infra.yml`, `specs/06-files.md`
