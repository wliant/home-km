# API reference (OpenAPI)

| Field | Value |
|---|---|
| Category | Non-functional · Documentation |
| Priority | P1 |
| Size | S |

**Current state:** Spec `11-api-conventions.md` describes envelope conventions (PageResponse, ErrorResponse) but no full endpoint reference. `frontend/src/api/` is the de facto contract.

**Gap:** Third-party integrations (a partner's home-automation hub, an operator's quick `curl`) have no documented API.

**Proposed direction:** Generate OpenAPI 3.1 from controllers via `springdoc-openapi`. Serve interactive Swagger UI at `/api/docs` (admin-only, off by default). Commit the YAML for diffability and frontend type generation (pairs with `testing/contract-testing.md`).

**References:** `backend/build.gradle.kts`, `backend/src/main/java/com/homekm/`, `specs/11-api-conventions.md`
