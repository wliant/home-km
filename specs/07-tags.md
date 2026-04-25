# Tags

## 1. Overview

Tags are user-defined labels that can be attached to notes, files, and folders. They live in a global registry shared across all household members. Tags have a name and a colour.

---

## 2. Tag Registry API

### `GET /api/tags`

List all tags.

**Auth required:** Yes

**Response (200):**
```json
[
  {
    "id": 1,
    "name": "Travel",
    "color": "#10b981",
    "createdAt": "2025-04-25T10:30:00Z"
  }
]
```

Not paginated — the household-scale tag list is small enough for a full load.

---

### `GET /api/tags/search`

Tag autocomplete.

**Auth required:** Yes

**Query parameters:**
- `q` — prefix string (1–100 chars), required

**Behaviour:** Returns up to 10 tags whose name contains `q` (case-insensitive). Uses the `gin_trgm_ops` index on `tags.name` for efficient matching.

**Response (200):** Same array shape as `GET /api/tags`, max 10 items.

---

### `POST /api/tags`

Create a new tag.

**Auth required:** Yes — adult only (child accounts cannot create tags)

**Request body:**
```json
{
  "name": "string",
  "color": "#10b981"
}
```

**Validation:**
- `name` — 1–100 chars, trimmed; required
- `color` — must match `^#[0-9A-Fa-f]{6}$`; `400 INVALID_COLOR` if malformed; defaults to `#6366f1` if omitted
- Duplicate name (case-insensitive): `409 TAG_NAME_EXISTS`

**Response (201):** Created tag object.

---

### `PUT /api/tags/{id}`

Update tag name and/or colour.

**Auth required:** Yes — adult only

**Request body:** Same as POST.

**Validation:** Same as POST. Duplicate name check excludes the current tag ID.

**Response (200):** Updated tag object.

---

### `DELETE /api/tags/{id}`

Delete a tag. Cascades to all `taggings` rows referencing this tag via DB CASCADE.

**Auth required:** Yes — adult only

**Response (204):** No content.

---

## 3. Attaching and Detaching Tags

### `POST /api/tags/{tagId}/attach`

Attach a tag to an entity.

**Auth required:** Yes — adult only (or note/file owner for child's own content — effectively adult only since tags are a write op blocked for children by `11-api-conventions.md`)

**Request body:**
```json
{
  "entityType": "note",
  "entityId": 42
}
```

**Validation:**
- `entityType` must be `note`, `file`, or `folder`; `400 INVALID_ENTITY_TYPE` otherwise
- `entityId` must refer to an existing entity of the specified type; `404 NOT_FOUND` if it does not
- Maximum 20 tags per entity; `400 TAG_LIMIT_EXCEEDED` if attaching would exceed this limit
- Idempotent: if the tagging already exists, return `200` (no error, no duplicate row)

**Response:** `201 Created` on new attachment, `200 OK` if already existed.

---

### `DELETE /api/tags/{tagId}/attach`

Remove a tag from an entity.

**Auth required:** Yes — adult only

**Request body:**
```json
{
  "entityType": "note",
  "entityId": 42
}
```

**Behaviour:** Silently succeeds if the tagging does not exist (idempotent).

**Response (204):** No content.

---

## 4. Inline Tag Management

The create and update endpoints for notes (`04-notes.md`), files (`06-files.md`), and folders (`03-folders.md`) all accept a `tagIds` array in the request body.

**Behaviour on create:** Insert a `taggings` row for each provided tag ID.

**Behaviour on update:** Diff the current set of tags against the provided `tagIds`:
- Tags in `tagIds` but not currently attached: insert new `taggings` rows
- Tags currently attached but not in `tagIds`: delete those `taggings` rows
- Tags in both: no change

This diff must be performed in the same transaction as the main entity update.

---

## 5. Validation Rules Summary

| Constraint | Rule | Error Code |
|-----------|------|-----------|
| Name length | 1–100 chars | `VALIDATION_ERROR` |
| Name uniqueness | Case-insensitive unique across all tags | `TAG_NAME_EXISTS` |
| Color format | `^#[0-9A-Fa-f]{6}$` | `INVALID_COLOR` |
| Tags per entity | Max 20 | `TAG_LIMIT_EXCEEDED` |
| Entity type | Must be `note`, `file`, or `folder` | `INVALID_ENTITY_TYPE` |

---

## 6. Tag Response Shape

```json
{
  "id": 1,
  "name": "Travel",
  "color": "#10b981",
  "createdAt": "2025-04-25T10:30:00Z"
}
```

When tags are embedded in note/file/folder responses, include only `id`, `name`, and `color` (omit `createdAt`).
