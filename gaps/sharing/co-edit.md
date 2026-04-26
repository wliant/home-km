# Real-time co-edit (long-term)

| Field | Value |
|---|---|
| Category | Functional · Sharing & collaboration |
| Priority | P2 |
| Size | L |

**Current state:** Two users editing the same note simultaneously experience a last-write-wins overwrite with no warning. No locking, no merge.

**Gap:** No collaborative editing.

**Proposed direction:** Long-term, integrate a CRDT library (`Y.js` with `y-websocket`) for note bodies only (checklists, tags, etc. continue to use REST). Defer until after `sync/realtime-updates.md` lands. Until then, the simpler win is a save-time conflict warning: include `If-Match: <etag>` on `PATCH /api/notes/{id}` and surface a "this note changed since you opened it" diff if the etag mismatches.

**References:** `backend/src/main/java/com/homekm/note/NoteService.java`, `frontend/src/features/notes/NoteEditorPage.tsx`, `specs/04-notes.md`
