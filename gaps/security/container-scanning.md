# Container image scanning (Trivy)

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P0 |
| Size | S |

**Current state:** Backend uses `eclipse-temurin:21-jre-alpine`; frontend uses `nginx:alpine` and `node:24-alpine` for build. CI builds images but does not scan them.

**Gap:** Base-image and OS-package CVEs (e.g., a vulnerable libcrypto) ship undetected.

**Proposed direction:** Add a Trivy job to CI that scans both built images, fails on `HIGH`+ for the runtime image and `CRITICAL` for the build image. Configure `.trivyignore` for accepted findings with expiry dates. Run on every PR and daily on `main`.

**References:** `.github/workflows/ci.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `specs/12-infrastructure.md`
