# Child-Safe Rules and Access Control

This spec defines the complete child-safe system. It is depended on by `03-folders.md`, `04-notes.md`, and `06-files.md`. Implement this spec before implementing those feature specs.

---

## 1. Definitions

| Term | Definition |
|------|-----------|
| **Adult user** | Any account where `is_child = false`, regardless of `is_admin` |
| **Child user** | Any account where `is_child = true` |
| **Child-safe item** | Any note, file, or folder where `is_child_safe = true` |
| **Child-unsafe item** | Any note, file, or folder where `is_child_safe = false` |

---

## 2. Access Control Rules (The Law)

These rules are absolute and must be enforced at the **service layer**, not in controllers.

**Rule 1 — Content filtering for children:**
Every API response for a child user MUST exclude any item where `is_child_safe = false`. This applies to:
- Folder tree and folder detail responses
- Note list and note detail responses
- File list and file detail responses
- Search results
- Any tag-based query that returns notes, files, or folders

**Rule 2 — 404 for unauthorised direct access:**
If a child user requests a specific resource by ID (e.g. `GET /api/notes/42`) and that resource is not child-safe, return `404 Not Found`. Do **not** return `403` — that would reveal the resource exists.

**Rule 3 — No child-unsafe content ever reaches a child's client:**
Backend is the authority. The frontend's UI hiding of unsafe content is a UX concern only; the backend enforces access independently.

---

## 3. Write Permissions for Child Accounts

Children have limited write access. All other write operations return `403 CHILD_ACCOUNT_READ_ONLY`.

**Allowed write operations for child accounts:**
- `POST /api/notes` — create own note (forced `is_child_safe = true`)
- `PUT /api/notes/{id}` — edit own note only (cannot change `is_child_safe`)
- `POST /api/files/upload` — upload own file (forced `is_child_safe = true`)
- `PUT /api/files/{id}` — edit metadata of own file only (cannot change `is_child_safe`)
- `PUT /api/files/{id}/content` — replace binary of own file only
- `POST /api/push/subscribe` / `DELETE /api/push/subscribe`
- `PUT /api/auth/me`

**Blocked for child accounts (return `403 CHILD_ACCOUNT_READ_ONLY`):**
- DELETE on any resource
- PUT on any resource not owned by the child
- `POST /api/folders`, `PUT /api/folders/{id}`, `DELETE /api/folders/{id}`
- Any admin endpoint
- Setting `is_child_safe = false` on any item
- Moving items between folders
- Attaching/detaching tags

**Ownership check for child edits:**
When a child attempts `PUT /api/notes/{id}`, verify `notes.owner_id = currentUser.id`. If not, return `403 FORBIDDEN` (not `CHILD_ACCOUNT_READ_ONLY` — distinguish "not your content" from "child account restriction").

---

## 4. Content Created by Children

All content created by a child account is automatically `is_child_safe = true`, regardless of what the request body specifies. The backend silently overrides the `is_child_safe` field to `true` and does not return an error.

Children cannot set `is_child_safe = false` on any item, including their own.

---

## 5. Cascade Rule: Marking a Folder Child-Safe

When `PUT /api/folders/{id}` changes `is_child_safe` from `false` to `true`, the following must happen in a **single database transaction**:

1. Set `is_child_safe = true` on the target folder
2. Set `is_child_safe = true` on all descendant folders (recursive CTE)
3. Set `is_child_safe = true` on all notes whose `folder_id` is in the descendant folder set (including the target folder)
4. Set `is_child_safe = true` on all files whose `folder_id` is in the descendant folder set (including the target folder)

**SQL pattern using recursive CTE:**
```sql
WITH RECURSIVE subtree AS (
    SELECT id FROM folders WHERE id = :folderId
    UNION ALL
    SELECT f.id FROM folders f
    JOIN subtree s ON f.parent_id = s.id
)
UPDATE folders SET is_child_safe = TRUE, updated_at = now()
    WHERE id IN (SELECT id FROM subtree);

UPDATE notes SET is_child_safe = TRUE, updated_at = now()
    WHERE folder_id IN (SELECT id FROM subtree);

UPDATE files SET is_child_safe = TRUE, updated_at = now()
    WHERE folder_id IN (SELECT id FROM subtree);
```

---

## 6. Cascade Rule: Marking a Folder NOT Child-Safe

When `PUT /api/folders/{id}` changes `is_child_safe` from `true` to `false`:

- **Only the target folder's flag is changed.**
- Descendant folders, notes, and files retain their current `is_child_safe` values.
- No cascade downward.

This is the deliberate asymmetry: marking safe propagates down; unmarking does not.

---

## 7. Cascade Rule: Adding a Child-Unsafe Item to a Child-Safe Folder

This rule applies when a note or file is **created** or **moved** into a folder where `is_child_safe = true`, and the item's `is_child_safe = false`.

Action: Set the immediate containing folder's `is_child_safe = false`.

**Scope:** Only the immediate parent folder is demoted. Ancestor folders are NOT affected.

**Example:** Folder A (safe) → Folder B (safe) → new unsafe note. Result: Folder B becomes unsafe. Folder A remains safe.

This is the only upward cascade in the system.

---

## 8. New Item Inheritance at Creation Time

When a note or file is created with a `folderId` that points to a folder with `is_child_safe = true`:
- The item inherits `is_child_safe = true` regardless of the value in the request body
- No error is returned; the override is silent

When a folder is created with a `parentId` pointing to a folder with `is_child_safe = true`:
- The new folder inherits `is_child_safe = true`

Exception: if the creating user is a child, `is_child_safe = true` is always forced regardless of folder state (Rule 4 above).

---

## 9. Item Move Inheritance

When a note or file is moved to a different folder (via `PUT /api/notes/{id}` or `PUT /api/files/{id}` with a new `folderId`):

- If the destination folder has `is_child_safe = true` and the item has `is_child_safe = false`:
  - Override item to `is_child_safe = true`
  - Do NOT demote the folder (the item is being made safe, not made unsafe)
- If the destination folder has `is_child_safe = true` and the item also has `is_child_safe = true`:
  - No change
- If the destination folder has `is_child_safe = false` and the item has `is_child_safe = true`:
  - Item retains `is_child_safe = true` (moving a safe item into an unsafe folder does not demote the item)
- If the destination folder has `is_child_safe = false` and the item has `is_child_safe = false`:
  - No change

---

## 10. Implementation Contract

**All cascade logic must be encapsulated in `ChildSafeService`.**

```java
public class ChildSafeService {
    void onFolderMarkedSafe(Long folderId);          // Rule from Section 5
    void onFolderMarkedUnsafe(Long folderId);        // Rule from Section 6 (no-op cascade)
    void onItemAddedToFolder(Long folderId, boolean itemIsChildSafe); // Rule from Section 7
    boolean resolveItemChildSafe(Long folderId, boolean requestedValue, boolean creatorIsChild); // Sections 4 & 8
}
```

- Controllers must not contain any child-safe cascade logic
- All cascade operations must be `@Transactional`
- Use single SQL UPDATE statements with recursive CTEs, not application-level loops

---

## 11. Testing Matrix

The following scenarios must be covered by unit tests in `ChildSafeService` (mocked repository) AND by integration tests with real DB. See `13-testing.md` for the full test matrix.

| # | Scenario | Expected Outcome |
|---|----------|-----------------|
| CS-1 | Create note with `is_child_safe=false` inside a child-safe folder | Note's flag overridden to `true` |
| CS-2 | Mark folder child-safe → verify all descendants and their notes/files | All become `is_child_safe = true` in DB |
| CS-3 | Add non-child-safe note to child-safe folder | Folder's `is_child_safe` set to `false` |
| CS-4 | Mark folder NOT child-safe | Descendants and their contents retain previous values |
| CS-5 | Child user `GET /api/notes/{id}` where note is `is_child_safe=false` | Returns `404 Not Found` |
| CS-6 | Child user `GET /api/folders` | Only child-safe folders returned |
| CS-7 | Child creates note with `is_child_safe=false` in request | Note saved with `is_child_safe=true` |
| CS-8 | Child attempts to delete a note | Returns `403 CHILD_ACCOUNT_READ_ONLY` |
| CS-9 | Child attempts to edit another user's note | Returns `403 FORBIDDEN` |
| CS-10 | Move safe note to unsafe folder | Note retains `is_child_safe=true` |
| CS-11 | Move unsafe note to safe folder | Note becomes `is_child_safe=true`; folder demoted to `false` does not apply (item was made safe) |
