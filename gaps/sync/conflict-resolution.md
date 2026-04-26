# Conflict resolution

| Field | Value |
|---|---|
| Category | Functional · Multi-device sync |
| Priority | P2 |
| Size | S |

**Current state:** No conflict detection on writes. A `PATCH` always wins regardless of intervening edits.

**Gap:** Two users editing the same note silently clobber each other.

**Proposed direction:** Add an `version BIGINT` column to `notes` (and other mutable rows). Increment on every update; controllers require it in the request body. Mismatch returns `409 CONFLICT` with both versions; the frontend surfaces a side-by-side merge. Pairs with `sharing/co-edit.md` for the long-term CRDT path on note bodies.

**References:** `backend/src/main/java/com/homekm/note/NoteService.java`, `backend/src/main/java/com/homekm/file/FileService.java`, `backend/src/main/java/com/homekm/folder/FolderService.java`, `frontend/src/features/notes/NoteEditorPage.tsx`, `specs/04-notes.md`
