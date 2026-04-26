# Operator runbook

| Field | Value |
|---|---|
| Category | Non-functional · Operability |
| Priority | P1 |
| Size | M |

**Current state:** Specs document the architecture; `CLAUDE.md` and `README.md` cover developer setup. There is no day-2 runbook for the operator (likely the household tech-savvy person, not a 24/7 ops team).

**Gap:** When something breaks, the operator is on their own with no guide.

**Proposed direction:** A `RUNBOOK.md` next to `README.md` covering: start/stop, viewing logs, taking a backup, restoring a backup, rotating `JWT_SECRET` (forces re-login of all users), changing the host certificate, updating to a new release, common error messages and what they mean, where to look for slow-query culprits, what to do when MinIO won't start. Cross-link from each NFR gap doc as relevant.

**References:** `README.md`, `CLAUDE.md`, `specs/12-infrastructure.md`
