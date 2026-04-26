# Result-type tabs

| Field | Value |
|---|---|
| Category | Functional · Search |
| Priority | P2 |
| Size | S |

**Current state:** Search results are a single chronological list of mixed notes, files, and folders. Tabbed switching requires re-querying with a `types` filter.

**Gap:** Hard to scan when looking for a specific kind ("show me only the files that match").

**Proposed direction:** Render result counts per type ("Notes 12 · Files 4 · Folders 1") and let the user click to filter the visible list without a new request — `SearchService` already returns all types when none are specified.

**References:** `frontend/src/features/search/SearchPage.tsx`, `backend/src/main/java/com/homekm/search/SearchService.java`, `specs/08-search.md`
