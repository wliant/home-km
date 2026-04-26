# Image push to registry

| Field | Value |
|---|---|
| Category | Non-functional · CI/CD |
| Priority | P0 |
| Size | S |

**Current state:** CI builds Docker images locally as a smoke test but does not push anywhere. Operators have to clone the repo and `docker compose build` on each host.

**Gap:** No prebuilt artifact. Slow upgrades, larger surface for "works in CI but not on host".

**Proposed direction:** On `main` and tag pushes, push images to GitHub Container Registry (`ghcr.io/wliant/home-km/api`, `.../frontend`). Tag with the git SHA, `latest`, and (for tags) the semver. `docker-compose.app.yml` switches default to `image: ghcr.io/...:latest` with `build:` as an override for developers.

**References:** `.github/workflows/ci.yml`, `docker-compose.app.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `specs/12-infrastructure.md`
