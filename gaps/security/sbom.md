# SBOM generation

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** No SBOM (Software Bill of Materials) generated for releases.

**Gap:** When a critical CVE drops (e.g., the next Log4Shell), the operator cannot quickly answer "do I run an affected version?".

**Proposed direction:** Generate a CycloneDX SBOM at build time using the `org.cyclonedx.bom` Gradle plugin (backend) and `@cyclonedx/cdxgen` (frontend). Upload to the GitHub Releases page alongside the image tag. Combined SBOM via `syft` against the final container images for transitive OS packages.

**References:** `backend/build.gradle.kts`, `frontend/package.json`, `.github/workflows/ci.yml`, `specs/12-infrastructure.md`
