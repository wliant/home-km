# Tag merge and rename cascade

| Field | Value |
|---|---|
| Category | Functional · Tags |
| Priority | P2 |
| Size | S |

**Current state:** Renaming a tag in `AdminController` updates only the `tags.name`; merging two tags (e.g., `recipe` and `recipes`) requires manual re-tagging.

**Gap:** No first-class merge. Drift accumulates as different users invent different spellings.

**Proposed direction:** Admin endpoint `POST /api/admin/tags/{sourceId}/merge` with `{targetId}` — moves all `taggings` from source to target inside one transaction (with `ON CONFLICT DO NOTHING` to dedupe), then deletes the source. Confirm-dialog UI on the admin tag manager. Rename is already trivial; just expose it in the same UI.

**References:** `backend/src/main/java/com/homekm/admin/AdminController.java`, `backend/src/main/java/com/homekm/tag/TagService.java`, `frontend/src/features/admin/`, `specs/07-tags.md`
