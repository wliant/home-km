# Install banner improvements

| Field | Value |
|---|---|
| Category | Functional · PWA & offline |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** The iOS install prompt component is shown to iOS users. Android users see only the browser's default `beforeinstallprompt` (often missed).

**Gap:** Inconsistent install affordance across platforms; no dismissal memory; no contextual prompt.

**Proposed direction:** Cross-platform `<InstallBanner>` component that captures `beforeinstallprompt` on Android/Chromium and renders a guided prompt on iOS. Suppress for 14 days after dismissal. Trigger only after meaningful engagement (3+ visits or first file upload), not on first load.

**References:** `frontend/src/components/IOSInstallPrompt.tsx`, `frontend/src/App.tsx`, `frontend/src/lib/`, `specs/10-offline-pwa.md`
