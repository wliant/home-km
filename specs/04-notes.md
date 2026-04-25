# Notes

## 1. Note Model

See `01-data-model.md` Section 4 for the full column definition. Key properties:

- **Body** — stored as raw Markdown text; max 100,000 characters. The backend does not parse or validate Markdown. The frontend renders it with `react-markdown`.
- **Label** — a categorisation tag (not a user-defined tag; see `07-tags.md`). Enforced as an enum at the application layer.
- **Checklist items** — a structured sub-resource on each note for interactive to-do/shopping-list behaviour. Separate from Markdown task-list syntax in the body.
- **Reminders** — sub-resource; see `05-reminders.md`.

---

## 2. Label Enum

Valid values for the `label` field:

| Value | Display Name |
|-------|-------------|
| `recipe` | Recipe |
| `todo` | To-Do |
| `reminder` | Reminder |
| `shopping_list` | Shopping List |
| `home_items` | Home Items |
| `usage_manual` | Usage Manual |
| `goal` | Goal |
| `aspiration` | Aspiration |
| `wish_list` | Wish List |
| `travel_log` | Travel Log |
| `custom` | Custom |

Implemented as a Java enum `NoteLabel` with a custom Spring `@ValidLabel` annotation. Invalid label values return `400 VALIDATION_ERROR` with:
```json
{
  "code": "INVALID_LABEL",
  "validValues": ["recipe", "todo", "reminder", "shopping_list", "home_items", "usage_manual", "goal", "aspiration", "wish_list", "travel_log", "custom"]
}
```

---

## 3. API Endpoints

### `GET /api/notes`

Paginated list of notes visible to the calling user.

**Auth required:** Yes

**Query parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `folderId` | Long | — | Filter by folder; omit for all notes; `null` keyword not supported (use folder endpoint's notes sub-resource for root notes) |
| `label` | String | — | Filter by label enum value |
| `page` | Integer | `0` | Zero-based page index |
| `size` | Integer | `20` | Page size, max `100` |

**Child account filter:** Only notes where `is_child_safe = true` are returned.

**Response (200):** Page envelope (see `11-api-conventions.md` Section 4) with note summary objects (no `body` field in list response — `body` is only in single-note response to reduce payload).

---

### `GET /api/notes/{id}`

Fetch a single note with full body, checklist items, reminders, and tags.

**Auth required:** Yes

**Child account rule:** Returns `404` if `is_child_safe = false` (see `09-child-safe.md` Rule 2).

**Response (200):** Full note object (see Section 6).

---

### `POST /api/notes`

Create a new note.

**Auth required:** Yes

**Request body:**
```json
{
  "folderId": 1,
  "title": "string",
  "body": "string or null",
  "label": "recipe",
  "isChildSafe": false,
  "checklistItems": [
    { "text": "string", "isChecked": false, "sortOrder": 0 }
  ],
  "tagIds": [1, 2]
}
```

**Validation:**
- `title` — 1–500 chars, required
- `body` — optional; max 100,000 chars; `400 BODY_TOO_LONG` if exceeded
- `label` — must be a valid enum value; defaults to `custom` if omitted
- `folderId` — must exist if provided; `400 NOT_FOUND` if it does not
- `checklistItems` — max 500 items; `400 CHECKLIST_LIMIT_EXCEEDED` if exceeded
- `tagIds` — max 20 tags; `400 TAG_LIMIT_EXCEEDED` if exceeded

**Child-safe rules:**
- If the creator is a child account: force `is_child_safe = true` (override silently)
- If `folderId` points to a child-safe folder: force `is_child_safe = true` (override silently)
- If the note is NOT child-safe and `folderId` points to a child-safe folder: demote the folder's `is_child_safe` to `false` (see `09-child-safe.md` Section 7)

All of the above are handled by `ChildSafeService.resolveItemChildSafe()`.

**Response (201):** Full note object.

---

### `PUT /api/notes/{id}`

Full update of a note.

**Auth required:** Yes

**Child account rules:**
- Child can only edit notes where `owner_id = currentUser.id`; otherwise `403 FORBIDDEN`
- Child cannot change `is_child_safe` to `false`; silently ignored if attempted

**Request body:** Same as POST.

**Business rules:**
- If `folderId` changes (move), apply child-safe inheritance rules for the new folder (see `09-child-safe.md` Section 9)
- `tagIds` diff: compute difference between current tags and provided list; insert new taggings, delete removed taggings in the same transaction
- Checklist items in the request body replace all existing checklist items for the note (full replace semantics). Use the checklist sub-resource endpoints for individual item operations.

**Response (200):** Full note object.

---

### `DELETE /api/notes/{id}`

Delete a note.

**Auth required:** Yes — adult only; child accounts cannot delete

**Rules:**
- Any adult can delete any note
- Cascades to `checklist_items`, `reminders`, `reminder_recipients`, and `taggings` via DB CASCADE

**Response (204):** No content.

---

## 4. Checklist Sub-Resource

### `GET /api/notes/{id}/checklist`

Returns checklist items sorted by `sort_order` ascending.

**Response (200):**
```json
[
  { "id": 1, "noteId": 1, "text": "Milk", "isChecked": false, "sortOrder": 0 },
  { "id": 2, "noteId": 1, "text": "Eggs", "isChecked": false, "sortOrder": 1 }
]
```

---

### `POST /api/notes/{id}/checklist`

Add a single checklist item.

**Auth required:** Yes — adults or note owner (child)

**Request body:**
```json
{ "text": "string", "sortOrder": 0 }
```

**Validation:** Total items after insert must not exceed 500. `400 CHECKLIST_LIMIT_EXCEEDED` if exceeded.

**Response (201):** Created checklist item.

---

### `PUT /api/notes/{id}/checklist/{itemId}`

Update a single checklist item.

**Auth required:** Yes — adults or note owner (child)

**Request body:**
```json
{ "text": "string", "isChecked": true, "sortOrder": 0 }
```

**Response (200):** Updated checklist item.

---

### `DELETE /api/notes/{id}/checklist/{itemId}`

Delete a single checklist item.

**Auth required:** Yes — adult only; child cannot delete

**Response (204):** No content.

---

### `PUT /api/notes/{id}/checklist/reorder`

Bulk update of `sort_order` values for all checklist items in a note. Used after drag-and-drop reordering in the UI.

**Auth required:** Yes — adults or note owner (child)

**Request body:**
```json
[
  { "id": 1, "sortOrder": 0 },
  { "id": 2, "sortOrder": 1 }
]
```

**Validation:**
- All provided IDs must belong to the specified note; `400 VALIDATION_ERROR` if any ID is foreign
- All items for the note must be present (no partial reorder)

**Response (204):** No content.

---

## 5. Business Rules Summary

| Rule | Detail |
|------|--------|
| Body max length | 100,000 characters |
| Checklist items max | 500 per note |
| Tags max | 20 per note |
| Child-safe inheritance | From folder at creation and move time |
| Delete cascade | checklist_items, reminders, taggings |
| Folder deleted | Note's `folder_id` set to `NULL` (ON DELETE SET NULL) |

---

## 6. Note Response Shape

Full note object (returned by `GET /api/notes/{id}`, `POST /api/notes`, `PUT /api/notes/{id}`):

```json
{
  "id": 1,
  "folderId": 1,
  "folderName": "Recipes",
  "ownerId": 1,
  "ownerDisplayName": "Jane",
  "title": "Pasta Carbonara",
  "body": "## Ingredients\n- 200g pasta\n...",
  "label": "recipe",
  "isChildSafe": true,
  "createdAt": "2025-04-25T10:30:00Z",
  "updatedAt": "2025-04-25T10:30:00Z",
  "checklistItems": [
    { "id": 1, "noteId": 1, "text": "Pasta", "isChecked": false, "sortOrder": 0 }
  ],
  "reminders": [
    {
      "id": 1,
      "remindAt": "2025-04-26T09:00:00Z",
      "recurrence": "weekly",
      "pushSent": false,
      "recipients": [
        { "userId": 1, "displayName": "Jane" }
      ]
    }
  ],
  "tags": [
    { "id": 1, "name": "Italian", "color": "#6366f1" }
  ]
}
```

List summary object (returned by `GET /api/notes` — no `body` or `checklistItems`):

```json
{
  "id": 1,
  "folderId": 1,
  "folderName": "Recipes",
  "ownerId": 1,
  "ownerDisplayName": "Jane",
  "title": "Pasta Carbonara",
  "label": "recipe",
  "isChildSafe": true,
  "checklistItemCount": 5,
  "checkedItemCount": 2,
  "hasReminders": true,
  "createdAt": "2025-04-25T10:30:00Z",
  "updatedAt": "2025-04-25T10:30:00Z",
  "tags": []
}
```
