# Structured JSON logs

| Field | Value |
|---|---|
| Category | Non-functional · Observability |
| Priority | P0 |
| Size | S |
| Status | Closed |

**Current state:** Spring's default Logback pattern produces human-readable text logs to stdout. MDC fields (`requestId`, `userId` from `MdcFilter`) appear inline but are not machine-parseable.

**Gap:** Aggregating logs in a tool like Loki, Elasticsearch, or Datadog requires lossy text parsing. Searching for "all errors for user 7" is brittle.

**Proposed direction:** Switch the production Logback config to use `logstash-logback-encoder`'s `LogstashEncoder` for stdout. MDC fields become first-class JSON keys. Keep the human-friendly pattern for dev (profile-based). Document the schema in the operator runbook (`non-functional/documentation/operator-runbook.md`).

**References:** `backend/src/main/resources/`, `backend/src/main/java/com/homekm/common/MdcFilter.java`, `backend/build.gradle.kts`, `specs/12-infrastructure.md`
