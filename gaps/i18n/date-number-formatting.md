# Date / number / currency formatting

| Field | Value |
|---|---|
| Category | Functional · i18n / l10n |
| Priority | P2 |
| Size | S |

**Current state:** Dates render via ad-hoc `toLocaleString()` calls or hand-formatted strings. No central helper. Currency rarely appears now, but receipts and budgets are foreseeable.

**Gap:** Inconsistent formats; "11/4/2026" is ambiguous (US vs EU).

**Proposed direction:** Centralize via `Intl.DateTimeFormat`, `Intl.NumberFormat`, `Intl.RelativeTimeFormat` in `frontend/src/lib/format.ts`. Lock format choice to the user's locale (set in `i18n/framework.md`) rather than the browser default. Replace ad-hoc calls progressively.

**References:** `frontend/src/lib/`, `frontend/src/`, `specs/14-frontend-architecture.md`
