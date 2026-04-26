# Comments and @mentions

| Field | Value |
|---|---|
| Category | Functional · Sharing & collaboration |
| Priority | P2 |
| Size | M |

**Current state:** Notes are single-author artifacts. Conversation about a note happens out-of-band (verbally, in a messaging app). No way to attach a question to a specific note or file.

**Gap:** No discussion thread; no `@mention` to ping another user.

**Proposed direction:** `comments` table (item_type, item_id, author_id, body, created_at, edited_at, deleted_at). `comment_mentions` join table. Mention triggers a Web Push to the mentioned user via existing `PushService`. UI: collapsible thread on note/file detail pages; bell icon shows unresolved mentions.

**References:** `backend/src/main/java/com/homekm/note/`, `backend/src/main/java/com/homekm/file/`, `backend/src/main/java/com/homekm/push/PushService.java`, `frontend/src/features/notes/NoteDetailPage.tsx`, `specs/04-notes.md`
