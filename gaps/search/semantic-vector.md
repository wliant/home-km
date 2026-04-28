# Semantic / vector search

| Field | Value |
|---|---|
| Category | Functional · Search |
| Priority | P1 |
| Size | L |
| Status | Closed |

**Current state:** `infra/postgres/init.sql` installs `pgvector`, and the schema reserves `embedding vector(1536)` columns on notes/files/folders. Nothing populates or queries them. `SearchService` only uses PostgreSQL `tsvector` keyword matching.

**Gap:** Searches for "thing for fixing the leaky tap" miss notes about "plumber's putty" because keyword overlap is zero. The infra cost has been paid; the feature has not been delivered.

**Proposed direction:** Add an embedding worker that listens for note/file changes and computes embeddings using a small local model (e.g., `bge-small-en-v1.5` via Ollama or a separate Python sidecar) — keeps the household-self-hosted ethos. Store in the existing `embedding` columns. Extend `SearchService` with a hybrid query: combine `ts_rank` (keyword) and `1 - (embedding <=> :queryVec)` (cosine sim) via reciprocal rank fusion. Surface a "Semantic" toggle in the search UI.

**References:** `backend/src/main/java/com/homekm/search/SearchService.java`, `infra/postgres/init.sql`, `backend/src/main/resources/db/migration/V001__init.sql`, `frontend/src/features/search/SearchPage.tsx`, `specs/08-search.md`
