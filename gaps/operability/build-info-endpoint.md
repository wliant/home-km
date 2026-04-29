# Build / version info endpoint

| Field | Value |
|---|---|
| Category | Non-functional · Operability |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** The running container exposes no version, commit SHA, or build timestamp. `actuator/info` is not in the exposed endpoints list.

**Gap:** Operator cannot quickly answer "what version is running?".

**Proposed direction:** Enable Spring Boot's `info` actuator and configure `info.git.mode=full` via `gradle-git-properties` plugin. Expose at `/actuator/info`. Frontend reads it on app load and shows version + commit in Settings → About so users can include it in bug reports.

**References:** `backend/src/main/resources/application.yml`, `backend/build.gradle.kts`, `frontend/src/features/settings/SettingsPage.tsx`, `specs/12-infrastructure.md`
