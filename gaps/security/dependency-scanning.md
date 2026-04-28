# Dependency scanning (OWASP DC, Snyk)

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P0 |
| Size | S |
| Status | Closed |

**Current state:** `.github/workflows/ci.yml` runs build and tests but no vulnerability scan against backend or frontend dependencies. CVEs in transitive dependencies ship undetected.

**Gap:** No visibility into known-vulnerable libraries.

**Proposed direction:** Add a `security-scan` job to CI: OWASP Dependency Check for Gradle (`org.owasp.dependencycheck` plugin) and `npm audit --omit=dev` plus `osv-scanner` for npm. Fail on `HIGH`+. Schedule a daily run on `main` so newly-disclosed CVEs surface even without commits. Pair with Renovate (`non-functional/cicd/renovate-dependabot.md`) for auto-PRs.

**References:** `.github/workflows/ci.yml`, `backend/build.gradle.kts`, `frontend/package.json`, `specs/13-testing.md`
