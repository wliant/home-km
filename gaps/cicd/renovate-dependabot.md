# Renovate / Dependabot

| Field | Value |
|---|---|
| Category | Non-functional · CI/CD |
| Priority | P0 |
| Size | S |

**Current state:** No automated dependency updates. Updates happen ad hoc when someone notices.

**Gap:** Dependencies drift; security patches lag.

**Proposed direction:** Enable Renovate (or Dependabot — Renovate has finer grouping). Configure: group all non-major patches into one weekly PR; major bumps as separate PRs; auto-merge patch PRs after CI green; manual review for major. Includes Docker base images, npm, Gradle, GitHub Actions.

**References:** `.github/workflows/ci.yml`, `backend/build.gradle.kts`, `frontend/package.json`, `backend/Dockerfile`, `frontend/Dockerfile`
