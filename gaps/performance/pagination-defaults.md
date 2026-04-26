# Pagination defaults audit

| Field | Value |
|---|---|
| Category | Non-functional · Performance |
| Priority | P2 |
| Size | S |

**Current state:** `PageResponse` is the standard envelope, but per-endpoint default and max page sizes are ad hoc. Some endpoints (tags, folders) may return all rows unbounded.

**Gap:** Risk of accidental N=ALL queries as households grow.

**Proposed direction:** Enforce a global default `size=25` and max `size=100` for paginated endpoints via a `@PageableDefault` audit. Endpoints that return collections without `Pageable` (folder tree, tags) should explicitly justify or migrate to pagination. Add a small unit test that pings each list endpoint with `size=1000` and asserts the response is clamped.

**References:** `backend/src/main/java/com/homekm/common/PageResponse.java`, `backend/src/main/java/com/homekm/note/NoteController.java`, `backend/src/main/java/com/homekm/file/FileController.java`, `backend/src/main/java/com/homekm/tag/TagController.java`, `specs/11-api-conventions.md`
