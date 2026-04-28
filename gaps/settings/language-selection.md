# Language selection

| Field | Value |
|---|---|
| Category | Functional · Settings |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** All UI strings are English literals. No language picker.

**Gap:** Households where English is not a primary language are excluded.

**Proposed direction:** Once `i18n/framework.md` lands, expose a Settings dropdown listing available locales (initially `en`, plus 1–2 demo locales). Choice persists per user. Backend error messages also localized once `i18n/backend-localization.md` ships.

**References:** `frontend/src/features/settings/SettingsPage.tsx`, `frontend/src/lib/`, `specs/14-frontend-architecture.md`
