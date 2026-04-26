# Secret management

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P1 |
| Size | M |

**Current state:** All secrets (`DB_PASSWORD`, `MINIO_SECRET_KEY`, `JWT_SECRET`, `VAPID_PRIVATE_KEY`) live in a single `.env` file on the host (referenced by both compose files). Anyone with shell access reads them.

**Gap:** No encryption at rest for secrets; no rotation mechanism; no separation between developer secrets and production secrets.

**Proposed direction:** Adopt Docker secrets (compose `secrets:`) for production deployments — secrets mounted as `/run/secrets/*` and read via env-pointer or a small init script. For multi-host setups, recommend SOPS (age-encrypted YAML in git) or integration with a self-hosted Vault. Document a rotation playbook (`JWT_SECRET` rotation requires invalidating all sessions). Defer external KMS until needed.

**References:** `docker-compose.app.yml`, `docker-compose.infra.yml`, `backend/src/main/java/com/homekm/common/AppProperties.java`, `specs/12-infrastructure.md`
