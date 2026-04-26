# Encryption at rest

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P1 |
| Size | M |

**Current state:** PostgreSQL data and MinIO objects are stored unencrypted on the Docker host. Anyone with disk access reads everything.

**Gap:** A stolen laptop or unencrypted cloud volume exposes the entire household's notes and files.

**Proposed direction:** Two layers. (1) Host: document and recommend full-disk encryption (LUKS / FileVault / BitLocker) — operator concern, not app code. (2) Object store: enable MinIO server-side encryption (SSE-S3 with auto-managed keys, or SSE-KMS for stricter setups). For especially-sensitive notes, optionally support per-note client-side encryption (long-term, P2). Document the choice in the operator runbook.

**References:** `docker-compose.infra.yml`, `infra/postgres/init.sql`, `backend/src/main/java/com/homekm/file/FileService.java`, `specs/12-infrastructure.md`, `specs/06-files.md`
