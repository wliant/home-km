# Archive / restore folders

| Field | Value |
|---|---|
| Category | Functional · Folders |
| Priority | P2 |
| Size | S |

**Current state:** Folders are either present or deleted. There is no in-between for "old but I don't want to throw it away" (e.g., last year's school papers).

**Gap:** No archive concept. Active folder list grows forever.

**Proposed direction:** Add `archived_at TIMESTAMPTZ NULL` on `folders`. Default folder list filters out archived folders; an "Archived" view shows them. Archiving a folder also hides its descendants from the active tree but keeps full-text search results (with a badge). Independent of soft-delete (`notes/soft-delete-trash.md`).

**References:** `backend/src/main/java/com/homekm/folder/Folder.java`, `backend/src/main/java/com/homekm/folder/FolderService.java`, `frontend/src/components/AppLayout.tsx`, `specs/03-folders.md`
