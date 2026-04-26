# ICU message format

| Field | Value |
|---|---|
| Category | Functional · i18n / l10n |
| Priority | P2 |
| Size | S |

**Current state:** N/A — no messages.

**Gap:** Plurals and gendered terms cannot be translated correctly with naive `string.replace` substitution.

**Proposed direction:** Use `react-i18next` with the ICU plugin (`i18next-icu`) so catalogs can encode plural rules: `{count, plural, one {# note} other {# notes}}`. Same for select cases (`{role, select, admin {Admin} child {Kid} other {Member}}`). Pays off as soon as translations are added.

**References:** `frontend/package.json`, `frontend/src/locales/`, `specs/14-frontend-architecture.md`
