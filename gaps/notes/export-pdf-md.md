# Note export (PDF / Markdown)

| Field | Value |
|---|---|
| Category | Functional · Notes |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** No export. Notes only exist inside the app.

**Gap:** Users cannot print a packing list, email a recipe, or hand a school-form note as a PDF. No way to get data out for occasional sharing.

**Proposed direction:** Two endpoints: `GET /api/notes/{id}/export?format=md` (raw markdown body + frontmatter for title/tags) and `?format=pdf` (server-side render via `flying-saucer` or `openhtmltopdf`). Frontend "Export" menu on note detail with both options. Pairs naturally with markdown rendering (`markdown-rendering.md`).

**References:** `backend/src/main/java/com/homekm/note/NoteController.java`, `frontend/src/features/notes/NoteDetailPage.tsx`, `specs/04-notes.md`
