# Shared / live shopping lists

| Field | Value |
|---|---|
| Category | Functional · Sharing & collaboration |
| Priority | P1 |
| Size | M |

**Current state:** Notes have a `shopping_list` label and checklist items. Two people opening the same shopping list don't see each other's check-offs in real time.

**Gap:** The single most-requested household-app workflow ("we're at the store, partner is at home, did anyone get milk?") is half-implemented.

**Proposed direction:** Push real-time updates of `ChecklistItem.is_checked` and `ChecklistItem` add/remove via the WebSocket/SSE channel from `sync/realtime-updates.md`. Optimistic UI on the client. Conflict policy: last-write-wins per item (the contention surface is tiny).

**References:** `backend/src/main/java/com/homekm/note/ChecklistItem.java`, `backend/src/main/java/com/homekm/note/NoteController.java`, `frontend/src/features/notes/NoteDetailPage.tsx`, `specs/04-notes.md`
