# Home KM — Claude Code Guide

## Project Overview

Self-hosted household knowledge management PWA. Spring Boot 3.2 / Java 21 backend, React 18 / TypeScript / Vite 5 frontend, PostgreSQL 15 (pgvector + pg_trgm), MinIO for file storage, Docker Compose for deployment.

## Repository Layout

```
backend/          Spring Boot API (Gradle Kotlin DSL)
frontend/         React PWA (Vite + Tailwind)
e2e/              Playwright E2E tests
infra/postgres/   PostgreSQL extension init SQL
scripts/          Utility scripts (VAPID key generation)
specs/            Engineering specifications (source of truth)
docker-compose.infra.yml   PostgreSQL + MinIO
docker-compose.app.yml     API + Frontend
.env              Local environment variables (not committed)
```

## Common Commands

### Backend

```bash
cd backend
./gradlew build            # compile + test
./gradlew test             # run all tests (unit + integration)
./gradlew bootRun          # start dev server on :8080
./gradlew jacocoTestReport # generate coverage report → build/reports/jacoco/
```

### Frontend

```bash
cd frontend
npm install
npm run dev       # Vite dev server on :5173 (proxies /api to :8080)
npm test          # Vitest watch mode
npm run build     # production build
npm run typecheck # tsc --noEmit
```

### Docker

```bash
# Start infrastructure (postgres + minio)
docker compose -f docker-compose.infra.yml up -d

# Start app (api + frontend)
docker compose -f docker-compose.app.yml up -d

# Rebuild after code changes
docker compose -f docker-compose.app.yml up -d --build

# Restart a single service
docker compose -f docker-compose.app.yml up -d --force-recreate api
```

## Architecture Conventions

### Backend package structure

Each feature lives in its own package under `com.homekm.<feature>/`:
- `Entity.java` — JPA entity
- `Repository.java` — Spring Data JPA repository
- `Service.java` — business logic
- `Controller.java` — REST endpoints
- `dto/` — request/response DTOs (Java records)

Shared code lives in `com.homekm.common/`: `AppProperties`, `GlobalExceptionHandler`, `PageResponse`, `ErrorResponse`, `MdcFilter`, etc.

### Frontend feature structure

Each feature lives in `src/features/<feature>/`:
- `<Feature>Page.tsx` — page component
- `<Feature>DetailPage.tsx` — detail view
- Co-located subcomponents

Shared code lives in `src/components/`, `src/lib/` (stores, utilities), `src/api/` (typed API modules), `src/types/`.

### API conventions

- All endpoints prefixed `/api/`
- Paginated lists return `PageResponse<T>` (content, page, size, totalElements, totalPages, first, last)
- Errors return `ErrorResponse` (code, message, timestamp, optional fieldErrors)
- Auth via `Authorization: Bearer <jwt>` header
- CORS origins configured via `CORS_ALLOWED_ORIGINS` env var (comma-separated)

## Environment Variables

Minimum required to start:
```
DB_USER=homekm
DB_PASSWORD=<your-password>
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=<your-password>
JWT_SECRET=<32+ character random string>
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

JWT_SECRET must be at least 32 characters — the app will throw `IllegalStateException` at startup otherwise.

## Security Notes

- SQL: use named parameters (`:param`) in all native queries — never string interpolation
- Filenames: always call `sanitizeFilename()` before storing — strips path separators
- Rate limiting: login endpoint is limited to 20 attempts per IP per 60 seconds via `LoginRateLimiter`
- CORS: Spring Security CORS rejects requests with non-matching `Origin` header with 403 — ensure `CORS_ALLOWED_ORIGINS` includes the actual browser origin (LAN IP if accessed over network)

## Testing

### Backend

- Unit tests in `src/test/java/com/homekm/` — use Mockito, no Spring context
- Integration tests use `@SpringBootTest` + Testcontainers (`pgvector/pgvector:pg15`) — require Docker
- Test config in `src/test/resources/application.yml` overrides secrets with safe test defaults

### Frontend

- Vitest + React Testing Library
- Auth store tests use `useAuthStore.setState(...)` directly (no mocks needed)
- API tests use `vi.mock('../../api/...')` — do not mock Zustand stores

## Known Constraints

- `APP_NAME` and `APP_THEME_COLOR` are injected at **build time** via Vite `define` — changing them requires a frontend rebuild
- Frontend Dockerfile uses `node:24-alpine` and `npm install` (not `npm ci`) because Alpine musl needs to resolve platform-specific optional deps at install time
- MinIO health check uses TCP probe (`/dev/tcp/localhost/9000`) because `curl` is not installed in the MinIO image
- API health check uses `wget` because `curl` is not installed in `eclipse-temurin:21-jre-alpine`
