# Tag color picker UX

| Field | Value |
|---|---|
| Category | Functional · Tags |
| Priority | P2 |
| Size | S |

**Current state:** `tags.color` exists in the schema but the admin UI exposes either no picker or a raw text input. No palette, no contrast preview.

**Gap:** Inconsistent or unreadable tag colors.

**Proposed direction:** Curated palette of 12 accessible color pairs (background + readable foreground), each meeting WCAG AA on white and dark backgrounds. Tag edit form renders the palette as swatches; "Custom" advances to a hex picker with a live contrast warning. Reuse the same palette for `folders/color-icon.md`.

**References:** `backend/src/main/java/com/homekm/tag/Tag.java`, `frontend/src/features/admin/`, `frontend/src/components/TagChip.tsx`, `specs/07-tags.md`
