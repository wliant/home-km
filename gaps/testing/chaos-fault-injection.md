# Chaos / fault injection

| Field | Value |
|---|---|
| Category | Non-functional · Testing & QA |
| Priority | P2 |
| Size | S |

**Current state:** No tests verify behavior when MinIO is down, when the DB is slow, or when the network drops mid-upload.

**Gap:** Reliability features (`reliability/circuit-breaker.md`, `reliability/retry-backoff.md`) ship untested for their actual failure modes.

**Proposed direction:** Add Toxiproxy as a sidecar in integration test fixtures. Existing `IntegrationTestBase` proxies MinIO and DB through it; new test methods inject latency, packet loss, and connection failures. Verify circuit breaker opens, retry occurs, and user-facing errors are clear.

**References:** `backend/src/test/java/com/homekm/integration/IntegrationTestBase.java`, `backend/src/main/java/com/homekm/file/FileService.java`, `specs/13-testing.md`
