# Contract testing (Pact / OpenAPI diff)

| Field | Value |
|---|---|
| Category | Non-functional · Testing & QA |
| Priority | P2 |
| Size | S |

**Current state:** Frontend and backend share no enforced contract. Manual coordination keeps DTOs in sync. Specs (`11-api-conventions.md`) describe conventions but no machine-checked schema.

**Gap:** A backend DTO renamed without updating the frontend ships a runtime error.

**Proposed direction:** Generate an OpenAPI spec from Spring controllers via `springdoc-openapi`. Commit the generated `openapi.yaml`. CI fails if the generated file differs from the committed one without an accompanying frontend type update. Frontend types generated via `openapi-typescript` from the same file.

**References:** `backend/build.gradle.kts`, `backend/src/main/java/com/homekm/`, `frontend/src/api/`, `specs/11-api-conventions.md`
