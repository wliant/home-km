# Frontend bundle analyzer

| Field | Value |
|---|---|
| Category | Non-functional · Performance |
| Priority | P2 |
| Size | S |

**Current state:** Vite produces an optimized build (lazy routes, code-split). No bundle-size monitoring; no tracking of regressions.

**Gap:** A future dependency (e.g., a heavy markdown library, PDF viewer) could silently double the initial JS payload.

**Proposed direction:** Add `rollup-plugin-visualizer` to the Vite build, output `stats.html`. Add a CI check using `bundlewatch` or `size-limit` that fails the build if main entry exceeds budget (e.g., 250KB gzipped initial). Dependencies introduced later (PDF preview, markdown renderer) belong on lazy chunks.

**References:** `frontend/vite.config.ts`, `frontend/package.json`, `.github/workflows/ci.yml`, `specs/14-frontend-architecture.md`
