# Saved searches

| Field | Value |
|---|---|
| Category | Functional · Search |
| Priority | P2 |
| Size | S |

**Current state:** Searches are one-shot. Re-running "all unpaid bills with reminder due this month" requires re-typing the query and re-applying filters every time.

**Gap:** No way to bookmark a query.

**Proposed direction:** Add `saved_searches` table (user_id, name, query JSON capturing q + filter state, sort, created_at). Endpoints: list/create/delete. Frontend: "Save this search" button when filters are non-empty; saved searches appear as quick chips above the search bar and as a sidebar group.

**References:** `backend/src/main/java/com/homekm/search/SearchController.java`, `frontend/src/features/search/SearchPage.tsx`, `specs/08-search.md`
