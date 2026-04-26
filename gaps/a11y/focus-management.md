# Focus management on route change

| Field | Value |
|---|---|
| Category | Functional · Accessibility |
| Priority | P2 |
| Size | S |

**Current state:** React Router transitions do not move keyboard focus. After clicking a sidebar link, focus stays on the sidebar item; screen-reader users hear nothing about the new page.

**Gap:** No focus restoration on navigation; no focus trap on modals; no focus return when modals close.

**Proposed direction:** A `RouteAnnouncer` component that, on each route change, moves focus to the new page's `<h1>` and announces the page title via an `aria-live` region. Use `react-aria` or `radix-ui` for any new modals/dialogs (built-in focus trap and return). Audit existing modal-like components.

**References:** `frontend/src/App.tsx`, `frontend/src/components/`, `frontend/package.json`, `specs/14-frontend-architecture.md`
