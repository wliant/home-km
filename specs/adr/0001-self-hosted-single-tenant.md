# ADR-0001: Self-hosted single-tenant scope

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | @wliant |

## Context

Home KM is intended for households of 2–6 members managing a few thousand files. The alternative path is a multi-tenant SaaS where one deployment serves many households. Multi-tenancy demands tenant-scoped row-level security on every query, per-tenant rate limits, billing, abuse handling, and 24/7 ops — none of which fit a side project that family members run on a NAS.

## Decision

The codebase assumes one deployment serves exactly one household. The only multi-user concept is the `is_admin` / `is_child` flag inside that household.

## Consequences

- **Positive:** No tenant column on every table. Queries stay simple. Auth gate is "are you signed in as a household member" — not "are you signed in as a household member who owns this row in *this* tenant". Rate limits, audit logs, backups all live at the host level.
- **Negative:** The architecture cannot pivot to SaaS without a schema migration on every table. Operators running multiple unrelated households must run multiple deployments.
- **Neutral:** RTO/RPO and SLO targets (`docs/slo.md`) are sized for one household. A bigger scale requires re-deriving those.

## Alternatives considered

- **Multi-tenant from day one.** Rejected — overkill for the target audience and would dominate the schema and the security model.
- **No accounts at all.** Rejected — content needs ownership for child-safe enforcement and audit.

## References

- `specs/00-overview.md` § 2 (Target scale, non-goals)
- `gaps/admin/household-group-concept.md` (P2 — re-evaluation point if multi-household ever lands)
