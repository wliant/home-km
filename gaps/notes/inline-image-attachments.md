# Inline image attachments in notes

| Field | Value |
|---|---|
| Category | Functional · Notes |
| Priority | P1 |
| Size | M |
| Status | Closed |

**Current state:** Notes and files exist in the same folder tree but are unrelated entities. To put a photo inside a note (e.g., a screenshot of an assembly diagram), users upload it as a file and reference it verbally.

**Gap:** No way to embed an image inside the note body. Markdown will render `![alt](url)` once `markdown-rendering.md` lands, but uploads need a one-step path.

**Proposed direction:** In the note editor, accept paste/drag-drop of images. On drop, upload to MinIO via existing `FileService` (folder = note's folder, link via a new `note_attachments` join table), insert a markdown image link at the cursor pointing to a new presigned-URL endpoint `GET /api/files/{id}/inline` that returns short-lived URLs scoped to the note.

**References:** `backend/src/main/java/com/homekm/file/FileService.java`, `backend/src/main/java/com/homekm/note/`, `frontend/src/features/notes/NoteEditorPage.tsx`, `specs/04-notes.md`, `specs/06-files.md`
