# Share link with expiry

| Field | Value |
|---|---|
| Category | Functional · Files |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** Files are only accessible to authenticated household users. To send a kid's school photo to a grandparent, the user has to download then attach via another channel.

**Gap:** No public, time-boxed share link. The vault is fully closed.

**Proposed direction:** Add `share_links` table (token, file_id, created_by, expires_at, max_downloads, downloads, password_hash NULL). Public endpoint `GET /s/{token}` serves a short presigned URL after optional password prompt. Default expiry 7 days. Audit each share creation. Admin can list and revoke active links.

**References:** `backend/src/main/java/com/homekm/file/FileController.java`, `backend/src/main/java/com/homekm/file/FileService.java`, `specs/06-files.md`
