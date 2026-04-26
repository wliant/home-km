# E2E on every PR

| Field | Value |
|---|---|
| Category | Non-functional · Testing & QA |
| Priority | P0 |
| Size | S |

**Current state:** `.github/workflows/ci.yml` declares the `e2e` job with `if: github.event_name == 'push' && (github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master')`. Pull requests skip E2E entirely.

**Gap:** Integration regressions slip into `main` and are caught only after merge. Slow feedback loop.

**Proposed direction:** Drop the `if:` guard so E2E runs on every PR. Sharded across 2–4 Playwright workers to keep total time under 10 minutes. Required check before merge. Failures upload Playwright traces as artifacts for fast diagnosis.

**References:** `.github/workflows/ci.yml`, `e2e/`, `specs/13-testing.md`
