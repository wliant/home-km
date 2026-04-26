# Image signing (cosign)

| Field | Value |
|---|---|
| Category | Non-functional · CI/CD |
| Priority | P2 |
| Size | S |

**Current state:** Once images are pushed (`image-push-registry.md`), they are unsigned. Operators cannot prove the image they pulled was actually built by this project's CI.

**Gap:** No supply-chain integrity verification.

**Proposed direction:** Sign each pushed image with cosign + GitHub OIDC keyless signing. Add a verification snippet to the operator runbook so cautious operators can `cosign verify ghcr.io/wliant/home-km/api:<sha> --certificate-identity=...`. Pairs naturally with SBOM (`security/sbom.md`).

**References:** `.github/workflows/ci.yml`, `specs/12-infrastructure.md`
