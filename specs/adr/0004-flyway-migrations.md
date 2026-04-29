# ADR-0004: Flyway over Liquibase for schema migrations

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | @wliant |

## Context

The backend persists to PostgreSQL and the schema evolves with every feature commit (`V001__init.sql` through V017+ at time of writing). A migration tool needs to:

- Run on every Spring Boot startup so a fresh deploy or `docker compose up` produces a usable DB.
- Track which scripts have run to avoid re-running them.
- Fail loudly if a checksum mismatch suggests a script was edited after deploy.

The two mainstream JVM choices are Flyway and Liquibase. Both meet the bar; the differences are syntax and feature set.

## Decision

Flyway runs on application startup (`spring.flyway.enabled=true`). Migration files live at `backend/src/main/resources/db/migration/V<NNN>__description.sql` in plain Postgres SQL. Repeatable migrations (R-prefix) and Java migrations are not used.

## Consequences

- **Positive:** Migrations are plain SQL. A DBA can read them without learning a DSL. The `psql` flow (`psql < V015__saved_searches.sql`) works for ad-hoc local runs. CI does not need a Liquibase changelog parser — Flyway's changelog is implicit in filenames.
- **Negative:** Flyway Community lacks rollbacks (paid feature). The recovery story is "write a forward-rolling Vxxx__revert_yyy.sql migration", which is mostly fine for a single-tenant household app but is a real cost for ops at scale.
- **Neutral:** Schema-as-code lives in the same repo as the Java that depends on it; PRs that need a column must include the migration.

## Alternatives considered

- **Liquibase.** Rejected — the changelog XML/YAML adds a layer of abstraction we don't need at this scale. The promise of database-agnostic DSL doesn't pay off when the only target is Postgres + pgvector + pg_trgm anyway.
- **JPA `hibernate.ddl-auto=update`.** Rejected — non-deterministic, will not back-fill data, and the team must own the migration history regardless.
- **Hand-rolled migrations.** Rejected — re-implementing Flyway's checksum + applied-versions table is a poor use of time.

## References

- `backend/src/main/resources/db/migration/`
- `backend/build.gradle.kts` (Flyway dependency)
- `specs/12-infrastructure.md`
