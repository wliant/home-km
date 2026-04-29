# Architecture Decision Records (ADRs)

| Field | Value |
|---|---|
| Category | Non-functional · Documentation |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** `specs/00-overview.md` includes a "Decision Log" (D1–D20). Future decisions made by PRs are not consistently recorded.

**Gap:** As the system evolves, the rationale behind choices fades. New contributors re-litigate decided questions.

**Proposed direction:** Adopt the lightweight ADR format under `specs/adr/` (`0001-use-flyway-not-liquibase.md`, etc.). Each ADR: status, context, decision, consequences. Rule: any change to the existing D1–D20 entries or any new architectural choice in CI requires an ADR PR. Tooling: `adr-tools` script for scaffolding.

**References:** `specs/00-overview.md`, `specs/`
