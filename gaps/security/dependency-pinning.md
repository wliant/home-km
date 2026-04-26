# Dependency pinning

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P2 |
| Size | S |

**Current state:** Backend uses Gradle `implementation(...)` declarations without version locking. Frontend `package.json` uses caret/tilde ranges. Dockerfiles pin major image tags (`node:24-alpine`, `nginx:alpine`) but not digests.

**Gap:** Builds are not reproducible. Same commit can pull different transitive deps months apart, and base-image rebuilds ship silent updates.

**Proposed direction:** Backend: Gradle dependency-locking (`./gradlew dependencies --write-locks`). Frontend: `npm ci` from a committed `package-lock.json` (already used in CI; CLAUDE.md explains why local Docker uses `npm install` for Alpine musl — keep that documented exception). Dockerfiles: pin base images by digest (`@sha256:...`) and refresh via Renovate.

**References:** `backend/build.gradle.kts`, `frontend/package.json`, `frontend/Dockerfile`, `backend/Dockerfile`, `CLAUDE.md`, `specs/12-infrastructure.md`
