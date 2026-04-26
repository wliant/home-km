# Search within current folder

| Field | Value |
|---|---|
| Category | Functional · Search |
| Priority | P1 |
| Size | S |

**Current state:** Global `/search` accepts a `folderId` filter, but there is no in-context "search this folder" affordance from `FolderPage`.

**Gap:** Users navigate to a folder, then have to leave it to search inside it. Friction for "I know it's somewhere in Recipes".

**Proposed direction:** Add a search input on `FolderPage` that pre-fills the global search with `folderId=current` and toggles "include subfolders" on by default. Reuse the existing search endpoint — no backend change.

**References:** `frontend/src/features/folders/FolderPage.tsx`, `frontend/src/features/search/SearchPage.tsx`, `backend/src/main/java/com/homekm/search/SearchService.java`, `specs/08-search.md`
