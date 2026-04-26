# Bulk tag operations

| Field | Value |
|---|---|
| Category | Functional · Tags |
| Priority | P2 |
| Size | S |

**Current state:** Tags are added/removed one at a time on each note/file/folder via the `TagAutocomplete` chip control.

**Gap:** No way to apply or remove a tag across many items at once (e.g., tag every 2024 receipt with `tax-2024`).

**Proposed direction:** Pair with the multi-select introduced in `folders/bulk-move.md`. Selection bar exposes "Add tag…" and "Remove tag…" actions. Server endpoint `POST /api/taggings:bulk` accepts `{items, addTagIds, removeTagIds}` in one transaction.

**References:** `backend/src/main/java/com/homekm/tag/TagService.java`, `backend/src/main/java/com/homekm/tag/TagController.java`, `frontend/src/components/TagAutocomplete.tsx`, `specs/07-tags.md`
