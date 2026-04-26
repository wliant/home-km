# Note templates

| Field | Value |
|---|---|
| Category | Functional · Notes |
| Priority | P2 |
| Size | S |

**Current state:** Notes are created blank. Recurring household formats (weekly meal plan, packing list, kid-handover checklist, recipe) must be retyped each time.

**Gap:** No template gallery, no "Save as template", no scaffolded structure for common note types.

**Proposed direction:** Mark a note `is_template BOOLEAN`. Templates list under `/notes/templates`. Creating from a template clones body, label, checklist items, and tags into a new note. Ship 4–5 built-in seed templates via Flyway (meal plan, shopping list, recipe, packing list, household incident log).

**References:** `backend/src/main/java/com/homekm/note/Note.java`, `backend/src/main/java/com/homekm/note/NoteController.java`, `frontend/src/features/notes/`, `specs/04-notes.md`
