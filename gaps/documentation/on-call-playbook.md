# On-call playbook

| Field | Value |
|---|---|
| Category | Non-functional · Documentation |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** No playbook. The operator (likely a household member, not a 24/7 ops team) has no shortlist of "what to do when the API is returning 500s".

**Gap:** Diagnosis depends on improvisation in the moment.

**Proposed direction:** A `PLAYBOOK.md` (or section of the runbook) with symptom → diagnosis → fix entries: API returns 500 → check `docker logs api` for stack; uploads fail → check MinIO health; notifications stop → check VAPID config; users locked out → check `LoginRateLimiter` cache; disk full → check log retention. Update as new failure modes are discovered.

**References:** `backend/src/main/java/com/homekm/auth/LoginRateLimiter.java`, `backend/src/main/java/com/homekm/push/PushService.java`, `specs/12-infrastructure.md`
