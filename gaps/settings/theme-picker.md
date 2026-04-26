# Theme / accent picker

| Field | Value |
|---|---|
| Category | Functional · Settings |
| Priority | P2 |
| Size | S |

**Current state:** Accent color is fixed at build time via `APP_THEME_COLOR` (default `#6366f1`). Changing it requires a frontend rebuild.

**Gap:** Different households can't easily personalize, and once `dark-mode.md` lands a single accent color won't read well on both modes.

**Proposed direction:** Move to CSS custom properties for accent (`--color-accent`, `--color-accent-fg`) defined per-mode. Settings page exposes a small palette of curated accents reusing the `tags/color-picker-ux.md` palette. Picked value persists in `localStorage` (or `users.preferences JSONB` once a server-side preferences blob exists).

**References:** `frontend/tailwind.config.ts`, `frontend/src/index.css`, `frontend/src/features/settings/SettingsPage.tsx`, `specs/00-overview.md`
