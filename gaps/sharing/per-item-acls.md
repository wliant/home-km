# Per-item access controls (ACLs)

| Field | Value |
|---|---|
| Category | Functional · Sharing & collaboration |
| Priority | P1 |
| Size | L |
| Status | Closed |

**Current state:** Visibility is binary household-wide except for the child-safe filter. Every adult sees every adult's notes and files. There is no concept of "private to me" or "shared with parent only".

**Gap:** Genuinely personal notes (medical, journal, gift planning for a partner) cannot stay private. Birthday surprises are impossible.

**Proposed direction:** Add `visibility ENUM('household','owner','custom')` plus `item_acls` join table (item_type, item_id, user_id, permission) for `custom`. Default remains `household` to preserve current behavior. Filter every list/search query through a centralized `AccessGuard`. Audit ACL changes (`admin/audit-log-viewer.md`). Update `SearchService` to apply the same predicate.

**References:** `backend/src/main/java/com/homekm/note/NoteService.java`, `backend/src/main/java/com/homekm/file/FileService.java`, `backend/src/main/java/com/homekm/folder/FolderService.java`, `backend/src/main/java/com/homekm/search/SearchService.java`, `backend/src/main/java/com/homekm/common/`, `specs/09-child-safe.md`
