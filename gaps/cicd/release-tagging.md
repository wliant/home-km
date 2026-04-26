# Release tagging + changelog

| Field | Value |
|---|---|
| Category | Non-functional · CI/CD |
| Priority | P1 |
| Size | S |

**Current state:** No release process. Operators upgrade by pulling `main`. No changelog, no release notes.

**Gap:** Operators cannot reason about "what changed since I last upgraded?". Difficult to roll back to a known-good version.

**Proposed direction:** Adopt Conventional Commits (already implicit in many commit messages). Use `release-please` or `changesets` to auto-generate `CHANGELOG.md` and cut tagged releases (`v0.x.y`). CI publishes images tagged with the version (`image-push-registry.md`). Operators upgrade by pinning to a tag.

**References:** `.github/workflows/ci.yml`, `specs/12-infrastructure.md`
