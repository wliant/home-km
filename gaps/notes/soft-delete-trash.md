# Soft delete + trash

| Field | Value |
|---|---|
| Category | Functional · Notes |
| Priority | P0 |
| Size | M |

**Current state:** `DELETE /api/notes/{id}` removes the row immediately. There is no undo, no trash bin, no retention period. The same applies to files and folders.

**Gap:** A single mis-tap permanently destroys data. No accidental-deletion safety net.

**Proposed direction:** Add `deleted_at TIMESTAMPTZ NULL` to `notes`, `files`, `folders`, with a partial index `WHERE deleted_at IS NULL`. Default queries filter it out. New endpoints: `POST /api/{type}/{id}/restore`, `GET /api/trash`. A scheduled job purges rows where `deleted_at < now() - 30 days`. Trash UI under `/trash` showing all soft-deleted items grouped by type.

**References:** `backend/src/main/java/com/homekm/note/NoteService.java`, `backend/src/main/java/com/homekm/file/FileService.java`, `backend/src/main/java/com/homekm/folder/FolderService.java`, `backend/src/main/resources/db/migration/V001__init.sql`, `specs/04-notes.md`, `specs/06-files.md`
