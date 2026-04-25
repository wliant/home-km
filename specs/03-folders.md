# Folders

## 1. Folder Tree Model

Folders form an unbounded-depth tree via the `parent_id` self-reference in the `folders` table. Root-level folders have `parent_id = NULL`. The "virtual root" (no folder entity) is the top-level scope — notes and files with `folder_id = NULL` exist at this level but are not inside any folder.

Maximum supported folder depth: 20 levels. Operations that would exceed depth 20 are rejected.

Notes, files, and sub-folders can all coexist within the same folder (mixed content).

---

## 2. API Endpoints

### `GET /api/folders`

Returns the full folder tree visible to the calling user.

**Auth required:** Yes

**Child accounts:** Only folders where `is_child_safe = true` are included. The tree is pruned — if a folder is not child-safe, it and all its descendants are omitted.

**Response (200):** Array of root-level folder objects, each with a recursive `children` array.

```json
[
  {
    "id": 1,
    "name": "Home",
    "description": "Household stuff",
    "parentId": null,
    "ownerId": 1,
    "ownerDisplayName": "Jane",
    "isChildSafe": true,
    "createdAt": "2025-04-25T10:30:00Z",
    "updatedAt": "2025-04-25T10:30:00Z",
    "tags": [],
    "children": [
      {
        "id": 2,
        "name": "Recipes",
        ...
        "children": []
      }
    ]
  }
]
```

---

### `GET /api/folders/{id}`

Returns a single folder with its immediate children listed (not recursively nested), plus counts of contained notes and files.

**Auth required:** Yes

**Child account rule:** Returns `404` if folder is not child-safe (see `09-child-safe.md` Section 2, Rule 2).

**Response (200):**
```json
{
  "id": 1,
  "name": "Home",
  "description": null,
  "parentId": null,
  "ownerId": 1,
  "ownerDisplayName": "Jane",
  "isChildSafe": true,
  "createdAt": "2025-04-25T10:30:00Z",
  "updatedAt": "2025-04-25T10:30:00Z",
  "tags": [],
  "noteCount": 4,
  "fileCount": 12,
  "children": [
    { "id": 2, "name": "Recipes", ... }
  ]
}
```

`children` here is the flat list of immediate child folders only (no recursive nesting).

---

### `GET /api/folders/{id}/breadcrumb`

Returns an ordered array from the root to the specified folder, inclusive.

**Auth required:** Yes

**Response (200):**
```json
[
  { "id": 1, "name": "Home" },
  { "id": 3, "name": "Travel" },
  { "id": 7, "name": "Japan 2025" }
]
```

**Error:** If folder depth exceeds 20, return `400` with `{"code": "FOLDER_TOO_DEEP"}`.

---

### `POST /api/folders`

Create a new folder.

**Auth required:** Yes — adult only (child accounts cannot create folders)

**Request body:**
```json
{
  "name": "string",
  "description": "string or null",
  "parentId": "number or null",
  "isChildSafe": false,
  "tagIds": [1, 2]
}
```

**Validation:**
- `name` — 1–255 chars, required
- `parentId` — must exist if provided; `400 NOT_FOUND` if it does not
- Depth check: if `parentId` is at depth 19 or greater, reject with `400 FOLDER_TOO_DEEP`
- Sibling uniqueness: `(COALESCE(parentId, 0), name)` must be unique; `409 FOLDER_NAME_CONFLICT` on duplicate

**Child-safe inheritance:**
- If `parentId` points to a child-safe folder, the new folder inherits `is_child_safe = true` regardless of the provided value (see `09-child-safe.md` Section 8)

**Response (201):** Created folder object (same shape as GET, without `children` or counts).

---

### `PUT /api/folders/{id}`

Update a folder's name, description, child-safe flag, parent (move), or tags.

**Auth required:** Yes — adult only

**Request body:**
```json
{
  "name": "string",
  "description": "string or null",
  "parentId": "number or null",
  "isChildSafe": false,
  "tagIds": [1, 2]
}
```

**Move validation** (when `parentId` changes):
- New parent must not be the folder itself: `400 FOLDER_CIRCULAR_REFERENCE`
- New parent must not be a descendant of the folder (cycle check via recursive CTE): `400 FOLDER_CIRCULAR_REFERENCE`
- Depth of new location must not exceed 20: `400 FOLDER_TOO_DEEP`
- Sibling uniqueness at the new parent must hold: `409 FOLDER_NAME_CONFLICT`

**Child-safe cascade** (when `isChildSafe` changes):
- `false → true`: cascade downward to all descendants and their content (see `09-child-safe.md` Section 5)
- `true → false`: only the folder itself is updated (see `09-child-safe.md` Section 6)

**Response (200):** Updated folder object.

---

### `DELETE /api/folders/{id}`

Delete a folder.

**Auth required:** Yes — adult only

**Query parameter:**
- `?force=true` — recursively deletes all descendant folders, notes, and files within this folder tree

**Without `?force`:**
- If the folder contains any child folders, notes, or files: `409 CONFLICT` with `{"code": "FOLDER_NOT_EMPTY"}`
- If the folder is empty: delete and return `204`

**With `?force=true`:**
- Delete all files from MinIO (iterate over all descendant files, call MinIO delete for each and its thumbnail)
- Delete all notes (cascades to checklist_items, reminders via DB CASCADE)
- Delete all descendant folders (bottom-up order to satisfy FK constraints, or use `ON DELETE RESTRICT` workaround with a recursive CTE delete order)
- Delete the target folder
- Return `204`

**Warning:** `?force=true` is a destructive, irreversible operation. It must be executed in a single transaction. If any step fails, the entire operation is rolled back. MinIO object deletion is not transactional — log any MinIO deletion failures but do not fail the API response if the DB operations succeeded (orphaned MinIO objects are a known trade-off).

---

## 3. Response Shape

Full folder response object:

```json
{
  "id": 1,
  "name": "string",
  "description": "string or null",
  "parentId": "number or null",
  "ownerId": 1,
  "ownerDisplayName": "string",
  "isChildSafe": false,
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601",
  "tags": [
    { "id": 1, "name": "string", "color": "#6366f1" }
  ]
}
```

The `children` array is only present on:
- `GET /api/folders` (recursive)
- `GET /api/folders/{id}` (flat immediate children only)

The `noteCount` and `fileCount` fields are only present on `GET /api/folders/{id}`.

---

## 4. Name Uniqueness

Sibling folders (those sharing the same `parent_id`, including `null`) must have unique names (case-sensitive). The database enforces this via a unique index on `(COALESCE(parent_id, 0), name)`.

On duplicate: `409 CONFLICT` with `{"code": "FOLDER_NAME_CONFLICT"}`.

---

## 5. Cycle Prevention

The following must be checked **before** any folder update that changes `parentId`:

```sql
WITH RECURSIVE descendants AS (
    SELECT id FROM folders WHERE id = :folderId
    UNION ALL
    SELECT f.id FROM folders f
    JOIN descendants d ON f.parent_id = d.id
)
SELECT id FROM descendants WHERE id = :newParentId
```

If this query returns a row, the new parent is a descendant of the folder — reject with `400 FOLDER_CIRCULAR_REFERENCE`.

---

## 6. Virtual Root

Notes and files at the virtual root level have `folder_id = NULL`. The virtual root itself has no database representation and no folder ID. It is not returned in any folder API response.

Frontend represents the virtual root as the top-level view (all folders with `parent_id = NULL` plus all notes/files with `folder_id = NULL`).
