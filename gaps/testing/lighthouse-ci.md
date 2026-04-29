# Lighthouse CI

| Field | Value |
|---|---|
| Category | Non-functional · Testing & QA |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** No automated performance/PWA/accessibility/best-practice scoring. Manual Lighthouse runs only.

**Gap:** PWA-quality regressions (missing manifest fields, broken offline) and performance regressions go unnoticed.

**Proposed direction:** Add a `lighthouse-ci` job to CI that runs against the built frontend served from a temporary nginx container. Assert minimum scores per category (performance ≥ 80, PWA = pass, accessibility ≥ 90 — pairs with `a11y/axe-ci.md`). Comment results on the PR.

**References:** `.github/workflows/ci.yml`, `frontend/Dockerfile`, `specs/13-testing.md`, `specs/14-frontend-architecture.md`
