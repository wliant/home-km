# Load tests (k6 / Gatling)

| Field | Value |
|---|---|
| Category | Non-functional · Testing & QA |
| Priority | P2 |
| Size | M |
| Status | Closed |

**Current state:** No load testing. Only functional/integration tests.

**Gap:** Unknown ceilings. Cannot answer "what happens when the whole household uploads vacation photos at once?".

**Proposed direction:** Author a small k6 script (`tests/load/`) for the highest-value endpoints: search, list notes, upload file, fire reminder. Run on-demand against a staging stack, not in main CI. Capture baseline numbers in a `LOAD-TESTING.md`. Re-run before each release so regressions are visible.

**References:** `backend/src/main/java/com/homekm/`, `specs/13-testing.md`
