# Content moderation hooks

| Field | Value |
|---|---|
| Category | Functional · Child-safe mode |
| Priority | P2 |
| Size | M |

**Current state:** Child accounts are restricted to items explicitly flagged `is_child_safe`. There is no scanning or automatic flagging — every item starts as adult-only and a parent must opt in.

**Gap:** No assistive moderation. A parent uploading 50 vacation photos must individually mark each as child-safe.

**Proposed direction:** On upload, run images through a basic NSFW classifier (e.g., `nsfwjs` or a small MobileNet-based model in a Python sidecar) and notes through a profanity wordlist. If "very likely safe", auto-flag. If borderline, surface in a parent review queue (`child-safe/parental-review-queue.md`). Always reversible by the parent.

**References:** `backend/src/main/java/com/homekm/file/FileService.java`, `backend/src/main/java/com/homekm/note/NoteService.java`, `backend/src/main/java/com/homekm/common/ChildSafeService.java`, `specs/09-child-safe.md`
