# Virus scan on upload

| Field | Value |
|---|---|
| Category | Functional · Files |
| Priority | P1 |
| Size | M |

**Current state:** Uploaded files are stored in MinIO and made downloadable via presigned URLs without any malware check. A child clicking a forwarded "homework.pdf" downloads whatever bytes were uploaded.

**Gap:** No anti-malware scanning. The vault is implicitly trusted.

**Proposed direction:** Add a `clamav` sidecar to `docker-compose.app.yml`. After upload, enqueue an async scan via `clamd` TCP. Store result in `stored_files.scan_status` (`pending`/`clean`/`infected`/`error`) and `scan_at`. Block downloads of `infected` and surface a banner. Optionally quarantine to a separate MinIO bucket. Re-scan periodically as virus definitions update.

**References:** `backend/src/main/java/com/homekm/file/FileService.java`, `backend/src/main/java/com/homekm/file/StoredFile.java`, `docker-compose.app.yml`, `specs/06-files.md`
