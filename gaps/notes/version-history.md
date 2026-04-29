# Note version history

| Field | Value |
|---|---|
| Category | Functional · Notes |
| Priority | P2 |
| Size | M |
| Status | Closed |

**Current state:** Each `PATCH /api/notes/{id}` overwrites the body. Previous versions are unrecoverable.

**Gap:** No way to recover a note after an accidental rewrite. No diff between versions.

**Proposed direction:** Add `note_revisions` table (note_id, body, title, label, edited_by, edited_at). Capture a revision row on every save (debounce client-side to avoid one-row-per-keystroke). Cap at last 50 revisions per note + revisions older than 90 days. Detail page gets a "History" tab with a simple diff (use `diff-match-patch`).

**References:** `backend/src/main/java/com/homekm/note/NoteService.java`, `frontend/src/features/notes/NoteDetailPage.tsx`, `specs/04-notes.md`
