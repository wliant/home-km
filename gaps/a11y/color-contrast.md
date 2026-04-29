# Color contrast audit

| Field | Value |
|---|---|
| Category | Functional · Accessibility |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** No documented contrast verification. The `APP_THEME_COLOR` (default indigo `#6366f1`) is configurable, and tag colors are picked freely — both can fall below WCAG AA thresholds.

**Gap:** Some color combinations may be unreadable for users with low vision or in bright sunlight.

**Proposed direction:** Audit core palette in both light and `dark-mode.md`. Validate `APP_THEME_COLOR` at build time (fail if contrast ratio with white text < 4.5). Tag color picker (`tags/color-picker-ux.md`) shows live contrast warnings. axe in CI (`a11y/axe-ci.md`) catches the rest.

**References:** `frontend/tailwind.config.ts`, `frontend/src/components/TagChip.tsx`, `frontend/src/index.css`, `specs/00-overview.md`
