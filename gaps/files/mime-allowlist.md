# MIME allowlist + magic-byte sniff

| Field | Value |
|---|---|
| Category | Functional · Files |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** `FileService` accepts any MIME type and trusts the client-supplied `Content-Type`. There is no server-side content-type detection.

**Gap:** A user (or attacker) can upload an `.exe` labelled as `image/png`, or any unsupported binary that bloats storage. No defense against MIME-confusion attacks against the browser preview path.

**Proposed direction:** Use Apache Tika's `MimeTypeDetector` to sniff the actual MIME from file bytes on upload. Reject if the sniffed type doesn't match the claimed type or is outside an allowlist (configurable: images, video, audio, PDF, common Office docs, plain text, archives). Persist the sniffed type as authoritative.

**References:** `backend/src/main/java/com/homekm/file/FileService.java`, `backend/src/main/java/com/homekm/file/StoredFile.java`, `specs/06-files.md`
