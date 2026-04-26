# axe in CI

| Field | Value |
|---|---|
| Category | Functional · Accessibility |
| Priority | P1 |
| Size | S |

**Current state:** No automated accessibility checks. CI runs typecheck, unit tests, build, and (on `main` only) Playwright E2E.

**Gap:** Accessibility regressions ship undetected.

**Proposed direction:** Add `@axe-core/playwright` to E2E and assert zero violations on a curated list of pages. Add a separate Lighthouse CI job that gates on accessibility score ≥ 90. Once `non-functional/testing/e2e-on-pr.md` lands, both run on every PR.

**References:** `e2e/`, `.github/workflows/ci.yml`, `specs/13-testing.md`
