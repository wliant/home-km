# Screen-reader testing pass

| Field | Value |
|---|---|
| Category | Functional · Accessibility |
| Priority | P2 |
| Size | M |
| Status | Closed |

**Current state:** No documented screen-reader testing. Spec `13-testing.md` does not list assistive-tech scenarios.

**Gap:** No evidence the app is usable with VoiceOver, NVDA, or TalkBack.

**Proposed direction:** Run through the top 10 user flows (login, create note, add reminder, upload file, search, mark child-safe, set offline pin) with VoiceOver (Mac/iOS) and NVDA (Windows). Catalog issues, fix systematically. Add the flows as Playwright + axe-playwright assertions for regression.

**References:** `e2e/`, `frontend/src/`, `specs/13-testing.md`
