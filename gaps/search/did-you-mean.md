# "Did you mean" / typo tolerance

| Field | Value |
|---|---|
| Category | Functional · Search |
| Priority | P2 |
| Size | S |

**Current state:** A search for `recip` matches via `pg_trgm` indexes on tag names but typos in note bodies (e.g., `electircity`) return zero results.

**Gap:** No typo tolerance, no suggestion of nearby terms.

**Proposed direction:** When a query returns zero results, fall back to a `pg_trgm` similarity query against indexed text columns. If the top match crosses a threshold, render "Did you mean: <term>" above the empty results. Cheap, no new dependency — `pg_trgm` is already enabled.

**References:** `backend/src/main/java/com/homekm/search/SearchService.java`, `infra/postgres/init.sql`, `frontend/src/features/search/SearchPage.tsx`, `specs/08-search.md`
