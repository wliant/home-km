# Graceful shutdown

| Field | Value |
|---|---|
| Category | Non-functional · Reliability |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** Spring Boot's default shutdown is immediate. Docker sends SIGTERM and waits 10s before SIGKILL. In-flight uploads can be cut mid-stream.

**Gap:** Rolling restarts and deploys cause user-visible failures.

**Proposed direction:** Set `server.shutdown=graceful` and `spring.lifecycle.timeout-per-shutdown-phase=30s` in `application.yml`. Increase Docker's `stop_grace_period` to 35s in `docker-compose.app.yml`. The `ReminderScheduler` should check a `shuttingDown` flag and skip new ticks. Document the rolling-restart playbook.

**References:** `backend/src/main/resources/application.yml`, `docker-compose.app.yml`, `backend/src/main/java/com/homekm/reminder/ReminderScheduler.java`, `specs/12-infrastructure.md`
