# Data export (GDPR-style)

| Field | Value |
|---|---|
| Category | Functional · Settings |
| Priority | P2 |
| Size | M |

**Current state:** No way to export a user's data. Even if a user wants to leave or back up their personal subset, they can only do it manually one item at a time.

**Gap:** Falls short of common data-portability expectations.

**Proposed direction:** `POST /api/me/export` enqueues a background job that streams the user's notes (markdown), checklist items, file metadata, tags, and reminders into a ZIP, plus the file blobs from MinIO. Email or in-app notification when ready. ZIP is downloaded once via a signed URL with 24h expiry.

**References:** `backend/src/main/java/com/homekm/auth/AuthController.java`, `backend/src/main/java/com/homekm/file/FileService.java`, `backend/src/main/java/com/homekm/note/NoteService.java`, `frontend/src/features/settings/SettingsPage.tsx`
