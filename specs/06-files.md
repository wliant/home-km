# Files

## 1. MinIO Object Key Scheme

All files are stored in a single MinIO bucket (name from `MINIO_BUCKET_NAME` env var, default `homekm`).

**Object key format:**
```
/{userId}/{folderSegment}/{fileId}/{filename}
```

Where:
- `userId` — the `files.owner_id` value
- `folderSegment` — the string `root` when `folder_id IS NULL`; otherwise the `folder_id` value as a string
- `fileId` — the `files.id` value (determined after DB insert)
- `filename` — the original filename, URL-safe transformed: spaces replaced with `_`, path separators (`/`, `\`) removed, URL-encoded

Example: user 3, folder 7, file ID 142, filename "Holiday Photo.jpg" → `/3/7/142/Holiday_Photo.jpg`

**Thumbnail key format:**
```
/{userId}/{folderSegment}/{fileId}/{filename}_thumb.jpg
```

Where `_thumb.jpg` is appended before the extension if the original has one, or appended directly if no extension. Example: `Holiday_Photo_thumb.jpg`.

---

## 2. Two-Phase Upload Insert

Because the MinIO key includes the database row ID, insert requires two steps:

1. Insert `files` row with `minio_key = 'pending'`, `size_bytes = 0`, `uploaded_at = now()`
2. Upload binary to MinIO using the key derived from the row ID
3. Update `files` row: set real `minio_key`, `size_bytes`, `uploaded_at = now()`

If step 2 or 3 fails, the `pending` row must be deleted in the error handler to avoid orphaned DB rows.

---

## 3. File Upload API

### `POST /api/files/upload`

Upload a file.

**Auth required:** Yes

**Request format:** `multipart/form-data`

**Form fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | Binary | Yes | File content |
| `folderId` | Long | No | Target folder ID; null = root |
| `description` | String | No | Free text description |
| `isChildSafe` | Boolean | No | Default `false` |
| `tagIds` | Long[] | No | Array of tag IDs |
| `clientUploadId` | UUID string | No | Idempotency key for offline uploads |

**Validation:**
- File size: max `MAX_FILE_UPLOAD_MB` MB (enforced by both Spring Boot and nginx)
- `folderId`: must exist if provided
- `tagIds`: max 20 tags; `400 TAG_LIMIT_EXCEEDED`

**Child-safe rules:**
- Child account: force `is_child_safe = true`
- Folder is child-safe: force `is_child_safe = true`
- Unsafe file into child-safe folder: demote folder (see `09-child-safe.md`)

**Idempotency (`clientUploadId`):**
- If a `files` row already exists with `(owner_id, client_upload_id)` matching the current user and provided `clientUploadId`: return `200` with the existing file record (no re-upload)
- If no match: proceed with upload, return `201`

**Response (201 or 200):** File metadata object (see Section 6).

---

## 4. File Metadata API

### `GET /api/files`

Paginated list of files.

**Auth required:** Yes

**Query parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `folderId` | Long | — | Filter by folder |
| `mimeType` | String | — | MIME type prefix filter (e.g. `image/`) |
| `page` | Integer | `0` | Zero-based |
| `size` | Integer | `20` | Max `100` |

**Child account filter:** Only `is_child_safe = true` files returned.

**Response (200):** Page envelope with file metadata objects (no `thumbnailUrl` in list — use individual file endpoint to get the short-lived URL).

---

### `GET /api/files/{id}`

Fetch single file metadata.

**Auth required:** Yes

**Child account rule:** `404` if `is_child_safe = false`.

**Response (200):** Full file metadata object including `thumbnailUrl` (presigned, short-lived).

---

### `GET /api/files/{id}/download-url`

Generate a presigned download URL.

**Auth required:** Yes

**Child account rule:** `404` if `is_child_safe = false`.

**Response (200):**
```json
{
  "url": "https://minio-internal:9000/homekm/3/7/142/Holiday_Photo.jpg?X-Amz-...",
  "expiresAt": "2025-04-25T10:45:00Z"
}
```

URL expiry: `PRESIGNED_URL_EXPIRY_MINUTES` env var (default `15`).

---

### `PUT /api/files/{id}`

Update file metadata (not the binary).

**Auth required:** Yes

**Child account rules:**
- Child can only edit files where `owner_id = currentUser.id`
- Child cannot set `is_child_safe = false`

**Request body:**
```json
{
  "filename": "string",
  "description": "string or null",
  "folderId": "number or null",
  "isChildSafe": false,
  "tagIds": [1, 2]
}
```

**Rules:**
- `filename` (optional): updates the display name only; the MinIO object key is unchanged. Validation: ≤500 chars, must not be blank, sanitised to the basename (path separators stripped, e.g. `../etc/passwd` → `passwd`).
- `folderId` change (move): apply child-safe inheritance for target folder (see `09-child-safe.md` Section 9)
- `tagIds` diff applied in same transaction

**Response (200):** Updated file metadata object.

---

### `PUT /api/files/{id}/content`

Replace the file binary, keeping the same file ID and metadata.

**Auth required:** Yes

**Child account rules:** Child can only replace content of their own files.

**Request format:** `multipart/form-data`

**Form fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | Binary | Yes | New file content |

**Process:**
1. Upload new binary to a new MinIO key (same key scheme but new timestamp or version suffix; or overwrite same key)
   - Recommended: overwrite the same `minio_key` (MinIO PUT is idempotent)
2. Delete old thumbnail (if exists) from MinIO
3. Update `files` row: `size_bytes`, `mime_type`, `updated_at = now()`
4. Trigger thumbnail regeneration asynchronously

**Response (200):** Updated file metadata object.

---

### `DELETE /api/files/{id}`

Delete a file.

**Auth required:** Yes — adult only

**Process:**
1. Delete MinIO object for `minio_key`
2. Delete MinIO object for `thumbnail_key` (if not null)
3. Delete `files` row (cascades taggings via DB CASCADE)

If MinIO deletion fails: log at WARN level, but still delete the DB row (orphaned MinIO objects are acceptable; they can be cleaned up manually).

**Response (204):** No content.

---

## 5. Thumbnail Generation

**Trigger:** Asynchronous, after a successful file upload or binary replacement. Use Spring `@Async` on the thumbnail generation method.

**Condition:** `mime_type` starts with `image/`

**Process:**
1. Download the uploaded image from MinIO as a stream
2. Resize to max 256×256 pixels maintaining aspect ratio
3. Output as JPEG (regardless of input format)
4. Upload JPEG to MinIO at `thumbnail_key`
5. Update `files.thumbnail_key` in DB

**Library:** [Thumbnailator](https://github.com/coobird/thumbnailator) — `Thumbnails.of(inputStream).size(256, 256).outputFormat("JPEG").toOutputStream(outputStream)`

**Error handling:** Log at WARN level; leave `thumbnail_key = null`. The thumbnail failure must not fail the upload API response.

**Non-image files:** `thumbnail_key` remains `null`. The frontend uses a generic file type icon.

---

## 6. File Metadata Response Shape

```json
{
  "id": 142,
  "folderId": 7,
  "folderName": "Japan 2025",
  "ownerId": 3,
  "ownerDisplayName": "Jane",
  "filename": "Holiday Photo.jpg",
  "mimeType": "image/jpeg",
  "sizeBytes": 2048000,
  "description": "Sunset at Fushimi Inari",
  "isChildSafe": true,
  "thumbnailUrl": "https://...",
  "uploadedAt": "2025-04-25T10:30:00Z",
  "updatedAt": "2025-04-25T10:30:00Z",
  "tags": [
    { "id": 3, "name": "Travel", "color": "#10b981" }
  ]
}
```

`thumbnailUrl` is:
- A short-lived presigned MinIO URL (same expiry as download URL: `PRESIGNED_URL_EXPIRY_MINUTES`)
- `null` if `thumbnail_key IS NULL`
- Only present in single-file GET and upload response; omitted from list responses

---

## 7. MinIO Bucket Setup

The Spring Boot application must ensure the bucket exists on startup:

```java
if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
}
```

**Bucket policy:** Private. No public access policy. All access via presigned URLs.

**Frontend never contacts MinIO directly.** All MinIO interaction is through the Spring Boot API (upload via API, download via presigned URL generated by API). CORS on MinIO is not required.

---

## 8. Configuration

| Env Var | Description | Default |
|---------|------------|---------|
| `MINIO_ENDPOINT` | MinIO server URL | `http://minio:9000` |
| `MINIO_ACCESS_KEY` | MinIO access key | required |
| `MINIO_SECRET_KEY` | MinIO secret key | required |
| `MINIO_BUCKET_NAME` | Bucket name | `homekm` |
| `MAX_FILE_UPLOAD_MB` | Max upload size in MB | `100` |
| `PRESIGNED_URL_EXPIRY_MINUTES` | Presigned URL lifetime | `15` |
