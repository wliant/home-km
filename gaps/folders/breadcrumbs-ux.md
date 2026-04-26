# Breadcrumbs UX review

| Field | Value |
|---|---|
| Category | Functional · Folders |
| Priority | P2 |
| Size | S |

**Current state:** `FolderPage` shows the current folder name but no full ancestor path. Deep folders (`Household / Bills / Utilities / Electricity / 2025`) are hard to orient in.

**Gap:** No breadcrumbs. Users tap "back" repeatedly instead of jumping up two levels.

**Proposed direction:** Compute the ancestor chain server-side via the existing folder hierarchy, return it on `GET /api/folders/{id}` (or a dedicated `/breadcrumbs` endpoint). Frontend renders a clickable trail at the top of `FolderPage` and falls back to overflow menu on narrow screens.

**References:** `backend/src/main/java/com/homekm/folder/FolderService.java`, `frontend/src/features/folders/FolderPage.tsx`, `specs/03-folders.md`
