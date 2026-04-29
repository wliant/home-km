# ADR-0005: Resilience4j circuit breaker on MinIO calls

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | @wliant |

## Context

The backend calls MinIO synchronously from request-serving threads on file upload, download presign, thumbnail generation, and metadata reads. If MinIO becomes unhealthy (network blip, disk full, restart, version upgrade), naive retries pile threads on the same dead remote and saturate the Tomcat thread pool — taking down the rest of the API along with file endpoints.

The DB has analogous risk but Spring's existing connection pool (HikariCP) provides bounded concurrency + leak detection. MinIO has no equivalent built-in.

## Decision

Wrap MinIO client calls in a Resilience4j circuit breaker (`minio` instance, `application.yml`):
- Sliding window 20 calls.
- Open at ≥ 50% failure rate.
- Stay open 30s before transitioning to half-open.
- Retry policy: 3 attempts, 50ms initial delay, exponential backoff ×4, only on retryable IO/server exceptions.

When the breaker is open, file endpoints surface `503 STORAGE_UNAVAILABLE` (handled in `GlobalExceptionHandler`) rather than blocking on a dead remote. Health endpoint reports `MinioHealthIndicator` separately so Kubernetes-style readiness probes drain traffic correctly.

## Consequences

- **Positive:** A MinIO restart no longer takes down the entire API. Other endpoints (notes, search, auth) remain fast. The breaker auto-resets after 30s of healthy calls.
- **Negative:** Operators see `503` on the file UI during a MinIO outage instead of a long wait. This is intentional — the runbook (`docs/RUNBOOK.md`) documents how to interpret it.
- **Neutral:** No equivalent for the DB because HikariCP + Spring's existing exception handling already cover the failure modes.

## Alternatives considered

- **No circuit breaker, longer client timeout.** Rejected — pile-up risk on a thread-per-request server.
- **Async / reactive MinIO calls.** Rejected — would force a partial migration to WebFlux for one slice of the stack.
- **Bulkhead pattern (separate thread pool for MinIO).** Considered as a complement; deferred until we have evidence the current setup has thread-pool exhaustion under partial-degraded MinIO.

## References

- `backend/src/main/resources/application.yml` (resilience4j config)
- `backend/src/main/java/com/homekm/common/MinioGateway.java`
- `backend/src/main/java/com/homekm/common/MinioHealthIndicator.java`
- `gaps/reliability/circuit-breaker.md` (closed)
- `backend/src/test/java/com/homekm/integration/ChaosToxiproxyTest.java`
