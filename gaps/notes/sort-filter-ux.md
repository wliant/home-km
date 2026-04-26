# Richer sort and filter UX

| Field | Value |
|---|---|
| Category | Functional · Notes |
| Priority | P1 |
| Size | S |

**Current state:** Notes list is paginated and orderable by recent update. No UI to filter by label, tag, owner, date range, or "has reminder".

**Gap:** Once a household has hundreds of notes, finding "all IDEAs from last month" or "shopping lists owned by mum" requires the global search box, which only matches text.

**Proposed direction:** Add a filter chip row above the notes list with: label (TODO/IMPORTANT/IDEA/shopping_list), owner (when sharing exists), tag autocomplete, date range, "has reminder", and "is pinned". Server-side: extend `GET /api/notes` query params (already accepts pagination — add `label`, `tagId`, `ownerId`, `since`, `until`, `hasReminder`).

**References:** `backend/src/main/java/com/homekm/note/NoteController.java`, `frontend/src/features/notes/NotesListPage.tsx`, `specs/04-notes.md`
