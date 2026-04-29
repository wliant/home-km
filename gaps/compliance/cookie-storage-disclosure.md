# Cookie / storage disclosure

| Field | Value |
|---|---|
| Category | Non-functional · Compliance & data |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** The frontend stores the JWT in `localStorage` (`authStore` with persist middleware) and uses IndexedDB for the offline upload queue. No disclosure of what's stored or why.

**Gap:** Even on a self-hosted family app, transparency about local storage helps adoption and trust.

**Proposed direction:** A small Settings → Privacy panel listing what is stored client-side (auth token, queued uploads, cached notes once `pwa/offline-read-notes.md` lands), with a "Clear local data" button that clears all of it and signs out. Document in a public `PRIVACY.md`.

**References:** `frontend/src/lib/authStore.ts`, `frontend/src/lib/offlineDb.ts`, `frontend/src/features/settings/SettingsPage.tsx`, `specs/14-frontend-architecture.md`
