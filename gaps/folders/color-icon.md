# Folder color and icon

| Field | Value |
|---|---|
| Category | Functional · Folders |
| Priority | P2 |
| Size | S |

**Current state:** All folders render as identical generic folder icons. Distinguishing "Recipes" from "Receipts" in a glance requires reading the label.

**Gap:** No visual differentiation. Sidebar quickly becomes a wall of identical rows.

**Proposed direction:** Add `color VARCHAR(7) NULL` (hex) and `icon VARCHAR(32) NULL` (named lucide-react icon) on `folders`. UI: small color swatch + icon picker on folder edit. Tree and list views render the chosen icon and color tint.

**References:** `backend/src/main/java/com/homekm/folder/Folder.java`, `frontend/src/features/folders/`, `specs/03-folders.md`
