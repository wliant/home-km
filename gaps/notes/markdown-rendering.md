# Markdown rendering pipeline

| Field | Value |
|---|---|
| Category | Functional · Notes |
| Priority | P0 |
| Size | S |
| Status | Closed |

**Current state:** Notes store raw text in `body`. The detail page renders the body as plain text (line breaks preserved). No markdown formatting, links, code blocks, or task lists are recognized.

**Gap:** Users who write `**bold**` or `# Heading` see literal symbols. The product is "freeform notes" but lacks the basic formatting users expect from any modern note app.

**Proposed direction:** Render note bodies through `react-markdown` + `remark-gfm` for tables/strikethrough/task-lists, and `rehype-sanitize` to strip dangerous HTML. Keep storage as plain text/markdown (no schema change). Add a split-pane edit/preview toggle on `NoteEditPage`. Verify XSS safety before shipping.

**References:** `frontend/src/features/notes/NoteDetailPage.tsx`, `frontend/src/features/notes/NoteEditorPage.tsx`, `backend/src/main/java/com/homekm/note/Note.java`, `specs/04-notes.md`
