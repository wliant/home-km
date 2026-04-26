# Suggested tags

| Field | Value |
|---|---|
| Category | Functional · Tags |
| Priority | P2 |
| Size | M |

**Current state:** `TagAutocomplete` only matches what the user types. Tagging a brand-new note/file requires the user to remember which tags exist and choose them.

**Gap:** No proactive suggestion. Useful tags go unused because users forget they're available.

**Proposed direction:** Cheap v1: suggest tags already attached to other items in the same folder. v2: suggest based on co-occurrence — "items tagged X are usually also tagged Y". Long-term, tie into semantic search (`search/semantic-vector.md`) to suggest from text content. Render suggestions as ghost chips below the autocomplete with a single tap to apply.

**References:** `frontend/src/components/TagAutocomplete.tsx`, `backend/src/main/java/com/homekm/tag/TagService.java`, `specs/07-tags.md`
