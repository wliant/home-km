# Initial locale set

| Field | Value |
|---|---|
| Category | Functional · i18n / l10n |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** No locales. Default English only.

**Gap:** Even with the framework in place, no shipped translations.

**Proposed direction:** Ship `en` (canonical) + 1–2 demo locales chosen by household need (likely `es` and `de` — common in self-hosted-app communities). Translations bootstrapped with a translation tool but reviewed by a fluent speaker before release. Document the contribution path for new locales in `specs/14-frontend-architecture.md`.

**References:** `frontend/src/locales/`, `frontend/src/`, `specs/14-frontend-architecture.md`
