# i18n framework

| Field | Value |
|---|---|
| Category | Functional · i18n / l10n |
| Priority | P1 |
| Size | M |

**Current state:** All UI strings are inline English literals. No translation framework; no extraction tooling; no message keys.

**Gap:** Localization is impossible without first introducing infrastructure.

**Proposed direction:** Adopt `react-i18next` (industry standard, small bundle, TanStack Query-friendly). Initial setup: `i18next-browser-languagedetector`, JSON catalogs under `frontend/src/locales/<lang>/<namespace>.json`, namespace per feature folder. Replace strings in `AppLayout` and `LoginPage` first as proof; expand thereafter. Provide an `i18next-parser` script in `package.json` for extraction.

**References:** `frontend/src/`, `frontend/package.json`, `frontend/src/main.tsx`, `specs/14-frontend-architecture.md`
