# Pre-commit hooks (lint / format)

| Field | Value |
|---|---|
| Category | Non-functional · CI/CD |
| Priority | P2 |
| Size | S |

**Current state:** No pre-commit hooks. Lint and format issues are caught in CI (or worse, in review).

**Gap:** Bikeshed-y formatting churn in PRs; slow feedback for trivial issues.

**Proposed direction:** Add `lefthook` or `husky` config: run ESLint + Prettier on staged frontend files; run `spotless` (Google Java Format) on staged backend files; run `markdownlint` on staged `.md` files. Optional `--no-verify` escape for emergencies (CI still enforces).

**References:** `frontend/package.json`, `backend/build.gradle.kts`, `frontend/eslint.config.js`, `.github/workflows/ci.yml`
