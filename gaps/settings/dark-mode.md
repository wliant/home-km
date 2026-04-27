# Dark mode

| Field | Value |
|---|---|
| Category | Functional · Settings |
| Priority | P0 |
| Size | S |
| Status | Closed |

**Current state:** Hardcoded light theme. No `darkMode` config in Tailwind, no `prefers-color-scheme` handling, no per-user theme preference.

**Gap:** Bedside use of the app at 11pm blasts the room with white. Universally expected feature.

**Proposed direction:** Enable Tailwind's `darkMode: 'class'`, audit components for explicit colors and replace with `dark:` variants (mostly `bg-white` → `bg-white dark:bg-gray-900`, etc.). Theme preference (`auto` / `light` / `dark`) stored in `localStorage` and reflected on `<html>`. Defer per-user persistence to backend until other settings need it.

**References:** `frontend/tailwind.config.ts`, `frontend/src/App.tsx`, `frontend/src/components/AppLayout.tsx`, `frontend/src/index.css`, `specs/14-frontend-architecture.md`
