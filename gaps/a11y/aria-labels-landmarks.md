# ARIA labels and landmarks

| Field | Value |
|---|---|
| Category | Functional · Accessibility |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** A handful of ARIA attributes exist (3 `aria-*` references found across the frontend). No landmarks, most icon-only buttons lack `aria-label`, no `role="navigation"` on the sidebar, no `role="main"` on the content region.

**Gap:** Screen readers cannot meaningfully describe most of the UI.

**Proposed direction:** Add `<header>`, `<nav>`, `<main>`, `<aside>` semantic landmarks to `AppLayout`. Audit every icon-only button and add `aria-label`. Form inputs always have a `<label>`. Add a project-wide ESLint rule `jsx-a11y/recommended` and fix violations.

**References:** `frontend/src/components/AppLayout.tsx`, `frontend/src/components/`, `frontend/eslint.config.js`, `frontend/package.json`, `specs/14-frontend-architecture.md`
