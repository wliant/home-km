# Threat model

| Field | Value |
|---|---|
| Category | Non-functional · Documentation |
| Priority | P2 |
| Size | M |

**Current state:** No documented threat model. Security choices (bcrypt cost, login rate limit) exist without a traceable rationale.

**Gap:** Future contributors making security-relevant changes have no map of what's defended against and what isn't.

**Proposed direction:** A short STRIDE-format `THREAT-MODEL.md` covering the main assets (notes, files, credentials, JWT secret), the trust boundaries (browser ↔ API, API ↔ MinIO, API ↔ DB), and the threats considered + mitigated vs accepted. Pair with `specs/02-auth.md` and `specs/06-files.md`. Revisit yearly.

**References:** `backend/src/main/java/com/homekm/auth/`, `backend/src/main/java/com/homekm/file/`, `specs/02-auth.md`, `specs/06-files.md`
