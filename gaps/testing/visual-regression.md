# Visual regression testing

| Field | Value |
|---|---|
| Category | Non-functional · Testing & QA |
| Priority | P2 |
| Size | S |

**Current state:** No screenshot or visual diff tests. CSS regressions (broken layout, accidental color change) are caught only by humans during review.

**Gap:** Visual bugs slip through.

**Proposed direction:** Use Playwright's built-in `toHaveScreenshot()` for a small set of canonical pages (login, notes list, note detail, settings, file grid in light + dark mode). Pixel diff threshold tuned to avoid font-rendering noise. Snapshots committed to the repo; updates require explicit `--update-snapshots`.

**References:** `e2e/`, `specs/13-testing.md`
