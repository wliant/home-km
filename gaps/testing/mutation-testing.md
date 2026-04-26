# Mutation testing target

| Field | Value |
|---|---|
| Category | Non-functional · Testing & QA |
| Priority | P2 |
| Size | S |

**Current state:** JaCoCo coverage reports are produced (`./gradlew jacocoTestReport`) but the project has no documented coverage target and no mutation testing — coverage of executed lines doesn't prove the assertions actually catch bugs.

**Gap:** "Covered" code may have no effective tests.

**Proposed direction:** Add Pitest (`info.solidsoft.pitest` Gradle plugin). Run on critical packages (`auth`, `common`, `file`) with a 70% mutation-score threshold. Run weekly (not on every PR — too slow). Surface report alongside JaCoCo.

**References:** `backend/build.gradle.kts`, `backend/src/test/java/com/homekm/`, `specs/13-testing.md`
