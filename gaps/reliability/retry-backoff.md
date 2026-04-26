# Retry with exponential backoff

| Field | Value |
|---|---|
| Category | Non-functional · Reliability |
| Priority | P1 |
| Size | S |

**Current state:** Backend MinIO/DB calls have no retry on transient failures (network glitch, brief MinIO restart). Frontend retries are TanStack Query defaults (3 attempts, no backoff customization).

**Gap:** Transient failures surface as user-visible errors when a quick retry would have succeeded.

**Proposed direction:** Backend: Resilience4j Retry on MinIO calls only (DB transients are rare and best handled via Hibernate/JDBC config). Exponential backoff (50ms, 200ms, 800ms), max 3 attempts, only on `IOException` and 5xx-equivalent S3 errors — never on validation/4xx. Frontend: configure TanStack Query global defaults with backoff. Both must be idempotent — paired with `idempotency-keys.md`.

**References:** `backend/src/main/java/com/homekm/file/FileService.java`, `frontend/src/main.tsx`, `frontend/src/api/`, `specs/06-files.md`
