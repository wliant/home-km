# Chunked / resumable upload

| Field | Value |
|---|---|
| Category | Functional · Files |
| Priority | P1 |
| Size | M |
| Status | Closed |

**Current state:** `POST /api/files` is a single multipart request capped at `MAX_FILE_UPLOAD_MB` (default 100MB). The offline queue (`offlineDb.ts`) retries the entire upload on reconnect — a partially-transferred 80MB video starts from zero.

**Gap:** No chunked or resumable upload. Large files over flaky home WiFi or mobile networks frequently restart from byte 0. Cannot upload anything larger than the configured cap.

**Proposed direction:** Adopt `tus.io` resumable uploads. Add a tus controller in front of `FileService` (Java reference server: `tus-java-server`). Frontend uses `tus-js-client` instead of multipart `fetch`. Persist upload state in MinIO multipart upload IDs so resume survives a server restart.

**References:** `backend/src/main/java/com/homekm/file/FileController.java`, `backend/src/main/java/com/homekm/file/FileService.java`, `frontend/src/lib/offlineDb.ts`, `frontend/src/api/index.ts`, `specs/06-files.md`, `specs/10-offline-pwa.md`
