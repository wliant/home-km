# Background sync for note edits

| Field | Value |
|---|---|
| Category | Functional · PWA & offline |
| Priority | P1 |
| Size | M |
| Status | Closed |

**Current state:** `offlineDb.ts` queues file uploads when offline. Note edits made offline simply fail with a network error — the change is lost.

**Gap:** Asymmetric offline support: files are first-class offline citizens, notes are not.

**Proposed direction:** Extend the offline queue to handle note `PATCH` and `POST` bodies. Use the Background Sync API where available (Chromium PWAs) and fall back to a focus/reconnect listener (Safari). Persist optimistic state in IndexedDB so the UI keeps the edit on reload. Reconcile on sync; surface conflicts via `sync/conflict-resolution.md`.

**References:** `frontend/src/lib/offlineDb.ts`, `frontend/src/sw.ts`, `frontend/src/api/index.ts`, `frontend/src/features/notes/NoteEditorPage.tsx`, `specs/10-offline-pwa.md`
