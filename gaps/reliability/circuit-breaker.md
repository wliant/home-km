# Circuit breaker for MinIO

| Field | Value |
|---|---|
| Category | Non-functional · Reliability |
| Priority | P1 |
| Size | M |
| Status | Closed |

**Current state:** `FileService` calls MinIO directly with no protection. If MinIO becomes slow or unreachable, every file request blocks waiting on the SDK timeout, exhausting the request thread pool and potentially the whole API.

**Gap:** A single dependent service outage cascades into total app unavailability.

**Proposed direction:** Wrap MinIO calls in Resilience4j circuit breaker + bulkhead. Open the breaker after N consecutive failures or a high error rate; fail fast for the cool-down window with a clear `503 STORAGE_UNAVAILABLE` error. Frontend renders a "File service temporarily unavailable" banner. Notes/folders/tags continue to work because they don't depend on MinIO.

**References:** `backend/src/main/java/com/homekm/file/FileService.java`, `backend/build.gradle.kts`, `backend/src/main/java/com/homekm/common/GlobalExceptionHandler.java`, `specs/06-files.md`
