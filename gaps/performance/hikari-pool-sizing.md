# HikariCP pool sizing review

| Field | Value |
|---|---|
| Category | Non-functional · Performance |
| Priority | P2 |
| Size | S |

**Current state:** `application.yml` sets `hikari.maximum-pool-size: 10`. Default minimum-idle, no leak detection, no connection timeout customization.

**Gap:** 10 connections is a reasonable default but unverified. No alarm if connections leak.

**Proposed direction:** Document the rule of thumb (`pool_size = ((core_count * 2) + effective_spindle_count)`). Add `leak-detection-threshold: 30000` (ms) so leaks log a stack trace. Once metrics exist (`observability/metrics-prometheus.md`), Hikari's metrics auto-export — set an alert on saturation > 80% sustained.

**References:** `backend/src/main/resources/application.yml`, `docker-compose.infra.yml`, `specs/12-infrastructure.md`
