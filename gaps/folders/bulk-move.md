# Bulk move (notes / files / folders)

| Field | Value |
|---|---|
| Category | Functional · Folders |
| Priority | P1 |
| Size | S |

**Current state:** Each note, file, and subfolder must be moved one at a time via its own detail page or per-item action.

**Gap:** Reorganizing a folder of 30 receipts into year-based subfolders takes 30 round trips.

**Proposed direction:** Add multi-select on `FolderPage` (long-press / shift-click), with a "Move to..." action opening the folder picker. Server endpoint `POST /api/items/move` accepting `{items: [{type, id}], targetFolderId}` in one transaction. Validates child-safe rules across all items at once.

**References:** `frontend/src/features/folders/FolderPage.tsx`, `backend/src/main/java/com/homekm/folder/FolderController.java`, `backend/src/main/java/com/homekm/note/NoteService.java`, `backend/src/main/java/com/homekm/file/FileService.java`, `specs/03-folders.md`
