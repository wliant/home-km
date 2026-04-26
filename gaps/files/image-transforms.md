# Image transforms (WebP/AVIF, multiple sizes)

| Field | Value |
|---|---|
| Category | Functional · Files |
| Priority | P1 |
| Size | M |

**Current state:** `FileService` generates a single JPEG thumbnail per image at upload time. Original images are downloaded full-size for any preview larger than the thumb.

**Gap:** No responsive image variants. Mobile users on cellular data download a 12MP photo to display a 400px preview. No modern formats (WebP/AVIF) for bandwidth savings.

**Proposed direction:** Generate three derivatives at upload: `thumb` (256px), `preview` (1024px), and `display` (2048px), each in WebP (and AVIF where build-time `libheif` is available). Store keys in `stored_files.derivatives JSONB`. Frontend `<picture>` element picks format/size by viewport. Pair with on-the-fly fallback for missing derivatives.

**References:** `backend/src/main/java/com/homekm/file/FileService.java`, `frontend/src/features/files/`, `specs/06-files.md`
