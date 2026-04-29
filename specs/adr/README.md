# Architecture Decision Records

ADRs capture the *why* behind architectural choices so future contributors don't re-litigate them. Format follows the lightweight pattern from [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions) — `Status / Context / Decision / Consequences` plus an alternatives section.

## When to write one

You **must** add (or amend) an ADR when:
- Replacing a load-bearing dependency (e.g. swapping Spring Security, Postgres, MinIO, Workbox).
- Changing the auth posture (cookie sessions, OIDC, MFA).
- Adding a new persistent dependency (new datastore, new external service).
- Changing the deployment topology (multi-host, Kubernetes).
- Anything that breaks compatibility with the documented `RTO 4h / RPO 1h`, `MAX_FOLDER_DEPTH=20`, or other ratchets.

You **don't need** one for:
- Adding features that fit the existing patterns (a new feature package under `com.homekm.*`).
- Bug fixes, refactors that preserve the public contract.
- Doc-only changes.

## Drafting

1. Copy `0000-template.md` to `NNNN-short-slug.md` where `NNNN` is the next four-digit number.
2. Status `Proposed` → land alongside the implementation PR with status `Accepted`.
3. Old ADRs are never deleted. Superseded entries get `Superseded by ADR-YYYY` in the status line.

## Index

- [0001 — Self-hosted single-tenant scope](0001-self-hosted-single-tenant.md)
- [0002 — JWT in localStorage instead of HttpOnly cookies](0002-jwt-localstorage.md)
- [0003 — MinIO over filesystem for blob storage](0003-minio-blob-storage.md)
- [0004 — Flyway over Liquibase for schema migrations](0004-flyway-migrations.md)
- [0005 — Stateless backend with Resilience4j circuit breaker on MinIO](0005-circuit-breaker-minio.md)
