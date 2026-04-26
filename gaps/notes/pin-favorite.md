# Pin / favorite notes

> **Status:** Closed in commit `2835ccf` on `claude/implement-gap-feature-UZJxg`.

| Field | Value |
|---|---|
| Category | Functional · Notes |
| Priority | P1 |
| Size | S |

**Current state:** Notes sort by updated_at descending. There is no way to keep a frequently-used note (e.g., "Family Wifi Password", "Doctor numbers") at the top of the list.

**Gap:** No pin / star / favorite. High-value notes scroll out of view as the household adds more content.

**Proposed direction:** Add `pinned_at TIMESTAMPTZ NULL` to `notes`. List endpoint orders `pinned_at DESC NULLS LAST, updated_at DESC`. Pin/unpin endpoint: `POST /api/notes/{id}/pin` and `DELETE /api/notes/{id}/pin`. Frontend: star icon on each card; "Pinned" section above the main list when any pins exist.

**References:** `backend/src/main/java/com/homekm/note/Note.java`, `backend/src/main/java/com/homekm/note/NoteService.java`, `frontend/src/features/notes/NotesListPage.tsx`, `specs/04-notes.md`
