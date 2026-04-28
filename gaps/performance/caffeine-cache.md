# Caffeine cache for hot lookups

| Field | Value |
|---|---|
| Category | Non-functional · Performance |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** No backend caching layer. Every request to fetch the folder tree, the tag list, or a user record hits PostgreSQL.

**Gap:** Tag autocomplete, sidebar folder tree, and per-request user lookups (`JwtAuthFilter`) repeat the same queries thousands of times per day.

**Proposed direction:** Add `spring-boot-starter-cache` with Caffeine. Annotate hot read paths with `@Cacheable`: `TagService.list()`, `FolderService.tree()`, `UserService.findById()`. TTL 5 min, evict on the corresponding write paths via `@CacheEvict`. In-memory only — no Redis needed for single-instance.

**References:** `backend/src/main/java/com/homekm/tag/TagService.java`, `backend/src/main/java/com/homekm/folder/FolderService.java`, `backend/src/main/java/com/homekm/auth/JwtAuthFilter.java`, `backend/build.gradle.kts`, `specs/12-infrastructure.md`
