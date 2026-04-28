# In-app file preview (PDF / video / audio)

| Field | Value |
|---|---|
| Category | Functional · Files |
| Priority | P0 |
| Size | M |
| Status | Closed |

**Current state:** Image files render via thumbnail + full-size on the file detail page. PDFs, videos, audio files, and Office documents have no preview — clicking "Open" forces a download.

**Gap:** Common household docs (PDF receipts, school PDFs, GoPro clips, voice memos) all require a download-and-open round trip. UX feels primitive.

**Proposed direction:** Add `FilePreview` component that branches by MIME: PDFs render via `pdfjs-dist` with paginated viewer; video and audio use native `<video>`/`<audio>` with the existing presigned URL; Office docs fall back to download (or optional LibreOffice headless conversion to PDF as a follow-up).

**References:** `frontend/src/features/files/FileDetailPage.tsx`, `frontend/src/api/index.ts`, `backend/src/main/java/com/homekm/file/FileController.java`, `specs/06-files.md`
