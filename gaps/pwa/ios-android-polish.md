# iOS / Android PWA polish

| Field | Value |
|---|---|
| Category | Functional · PWA & offline |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** `IOSInstallPrompt` exists, manifest is configured, basic install works. iOS-specific quirks (status-bar color, safe-area insets, splash screens) are not all addressed; Android's Web Share Target is not implemented.

**Gap:** Installed-app feel falls short of native expectations.

**Proposed direction:** Add `apple-touch-startup-image` PNGs for common iPhone sizes; respect `env(safe-area-inset-*)` in `AppLayout`'s bottom nav padding; declare `display_override: ["window-controls-overlay"]` for desktop PWAs; register a `share_target` so other apps can share a file/text into Home KM (e.g., a screenshot directly into a folder).

**References:** `frontend/vite.config.ts`, `frontend/index.html`, `frontend/src/components/AppLayout.tsx`, `frontend/src/components/IOSInstallPrompt.tsx`, `specs/10-offline-pwa.md`
