# Advanced filters (date range, owner, file type)

| Field | Value |
|---|---|
| Category | Functional · Search |
| Priority | P1 |
| Size | S |

**Current state:** `SearchService.search(...)` accepts `q`, `types` (note/file/folder), `folderId`, and `tagIds`. No date range, no owner filter, no file-type filter, no "has reminder", no "child-safe only".

**Gap:** Power-search functionality is limited to existing structural filters.

**Proposed direction:** Extend `SearchController` query params: `since`, `until`, `ownerId`, `mimePrefix` (e.g., `image/`), `hasReminder`, `childSafe`. Translate into native SQL `WHERE` clauses. UI: a collapsible "Filters" panel with date pickers, owner dropdown, MIME chips. Persist last-used filters in URL params for shareable searches.

**References:** `backend/src/main/java/com/homekm/search/SearchService.java`, `backend/src/main/java/com/homekm/search/SearchController.java`, `frontend/src/features/search/SearchPage.tsx`, `specs/08-search.md`
