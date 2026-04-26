# File rename

> **Status:** Closed on branch `claude/implement-gao-feature-j8AWt` — the backend `PUT /api/files/{id}` already accepted `filename`; this branch added blank-filename rejection at the DTO level, an inline rename affordance on `FileDetailPage`, and an integration test covering happy path, path-traversal sanitisation, blank/empty rejection, and child-account authorisation. Spec `specs/06-files.md` updated to document `filename` in the PUT body.

| Field | Value |
|---|---|
| Category | Functional · Files |
| Priority | P1 |
| Size | S |

**Current state:** A file's display name is set at upload from the original filename and is immutable thereafter. To "rename", the user must delete and re-upload.

**Gap:** Common need (e.g., "IMG_3829.JPG" → "Kitchen tap warranty.jpg") is not supported.

**Proposed direction:** `PATCH /api/files/{id}` accepts `{filename}`. Validate via existing `sanitizeFilename()` in `FileService`. Display name is independent of the MinIO key — no object rewrite needed. Update `FileDetailPage` to show an inline editable filename, and the file list to use the same component.

**References:** `backend/src/main/java/com/homekm/file/FileController.java`, `backend/src/main/java/com/homekm/file/FileService.java`, `frontend/src/features/files/FileDetailPage.tsx`, `specs/06-files.md`
