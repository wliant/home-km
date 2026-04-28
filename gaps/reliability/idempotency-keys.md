# Idempotency keys on POST endpoints

| Field | Value |
|---|---|
| Category | Non-functional · Reliability |
| Priority | P1 |
| Size | M |
| Status | Closed |

**Current state:** Only file uploads use `clientUploadId` (`StoredFile.client_upload_id` is unique-indexed). All other write endpoints — create note, add reminder, attach tag — accept duplicate POSTs and create duplicate rows.

**Gap:** Network retry of any non-file POST creates duplicate data. The offline queue (`offlineDb.ts`) must therefore avoid replaying writes — it currently only handles uploads.

**Proposed direction:** Add an `Idempotency-Key` header convention for all `POST` endpoints. New table `idempotency_keys (key, user_id, response_body, response_status, created_at)`. A filter intercepts requests with the header, returns cached response if seen. Default TTL 24h. Unblocks safe retry on every write and broadens the offline queue beyond uploads.

**References:** `backend/src/main/java/com/homekm/file/FileService.java` (existing pattern), `backend/src/main/java/com/homekm/common/`, `frontend/src/lib/offlineDb.ts`, `specs/11-api-conventions.md`
