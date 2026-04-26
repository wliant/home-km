# Offline read for notes

| Field | Value |
|---|---|
| Category | Functional · PWA & offline |
| Priority | P1 |
| Size | M |

**Current state:** The Workbox service worker uses `NetworkFirst` for `/api/*` with cache fallback. Note bodies happen to be cached as a side effect, but there is no explicit guarantee, no UX for "this note is available offline", and the cache is opaque.

**Gap:** Users on the road or with flaky connections cannot rely on seeing yesterday's notes. The offline story today is upload queue only.

**Proposed direction:** Persist notes to IndexedDB explicitly via a `useOfflineNotes` hook backed by Dexie (already a peer of `offlineDb.ts`). Add a per-note "Available offline" pin that locks it in the cache. Bulk pin "All my pinned notes" and "All recent". Add a banner when displaying cached content older than X minutes.

**References:** `frontend/src/sw.ts`, `frontend/src/lib/offlineDb.ts`, `frontend/src/api/index.ts`, `frontend/vite.config.ts`, `specs/10-offline-pwa.md`
