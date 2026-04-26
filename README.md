# Home KM

A self-hosted household knowledge management Progressive Web App (PWA). A shared digital brain for a family or small household — a notebook for notes of all kinds and a file vault for documents and media.

## Features

- **Notebook** — freeform notes with labels, checklists, reminders, tags, and Markdown body
- **File vault** — documents and images stored in MinIO, organised in a shared folder tree
- **Full-text search** — ranked results with highlighted excerpts across notes and files
- **Reminders** — scheduled push notifications with daily/weekly/monthly/yearly recurrence
- **Tags** — trigram autocomplete, color-coded, shared across notes and files
- **Child-safe mode** — folder-level toggle that cascades to notes and files; child accounts only see safe content
- **Offline support** — IndexedDB upload queue that flushes when connectivity is restored
- **PWA** — installable, push notifications via Web Push (VAPID)
- **Multi-user** — admin can manage users, reset passwords, toggle roles

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + TypeScript + Vite 5 |
| Styling | Tailwind CSS v3 |
| State | Zustand (auth), TanStack Query v5 (server) |
| Forms | React Hook Form + Zod |
| PWA | vite-plugin-pwa (Workbox) |
| Backend | Spring Boot 3.2 + Java 21 + Gradle (Kotlin DSL) |
| Database | PostgreSQL 15 + pgvector + pg_trgm |
| File storage | MinIO |
| Migrations | Flyway |
| Frontend container | nginx:alpine |
| Runtime | Docker Compose |

## Quick Start

### Prerequisites

- Docker and Docker Compose
- (Optional) VAPID keys for push notifications — generate with `scripts/gen-vapid-keys.sh`

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env — set DB_PASSWORD, MINIO_SECRET_KEY, JWT_SECRET (≥32 chars), and optionally VAPID keys
```

### 2. Start infrastructure

```bash
docker compose -f docker-compose.infra.yml up -d
```

### 3. Start the app

```bash
docker compose -f docker-compose.app.yml up -d
```

### 4. Open in browser

Navigate to `http://localhost:3000` (or the configured `FRONTEND_PORT`).

The first registered account becomes the admin.

## Environment Variables

See `.env.example` for the full list with documentation comments. Key variables:

| Variable | Default | Description |
|---|---|---|
| `DB_PASSWORD` | — | PostgreSQL password (required) |
| `MINIO_ACCESS_KEY` | — | MinIO root user (required) |
| `MINIO_SECRET_KEY` | — | MinIO root password (required) |
| `JWT_SECRET` | — | HS256 signing secret, minimum 32 characters (required) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed origins |
| `VAPID_PUBLIC_KEY` | — | Web Push VAPID public key (optional) |
| `VAPID_PRIVATE_KEY` | — | Web Push VAPID private key (optional) |
| `VAPID_SUBJECT` | — | Web Push subject URI (optional) |
| `APP_NAME` | `Home KM` | Application name (injected at build time) |
| `APP_THEME_COLOR` | `#6366f1` | PWA theme color (injected at build time) |
| `FRONTEND_PORT` | `3000` | Host port for the frontend |
| `API_PORT` | `8080` | Host port for the backend API |

## Development

### Backend

```bash
cd backend
./gradlew bootRun
# Runs on :8080; requires postgres and minio from docker-compose.infra.yml
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# Runs on :5173 with Vite proxy to :8080
```

### Tests

```bash
# Backend unit + integration (Testcontainers — requires Docker)
cd backend && ./gradlew test

# Frontend unit tests
cd frontend && npm test -- --run

# E2E (Playwright — requires both services running)
cd e2e && npx playwright test
```

## Project Structure

```
.
├── backend/                  Spring Boot API
│   └── src/main/java/com/homekm/
│       ├── auth/             Authentication, JWT, rate limiting
│       ├── admin/            User management
│       ├── folder/           Folder tree
│       ├── note/             Notes + checklist items
│       ├── reminder/         Reminders + scheduler
│       ├── file/             File upload/download (MinIO)
│       ├── tag/              Tags + taggings
│       ├── search/           Full-text + vector search
│       ├── push/             Web Push subscriptions
│       └── common/           Shared utilities, error handling, MDC
├── frontend/                 React PWA
│   └── src/
│       ├── api/              Typed API client modules
│       ├── components/       Shared UI components
│       ├── features/         Feature-sliced pages
│       ├── lib/              Stores, offline queue, toast
│       └── types/            Shared TypeScript types
├── e2e/                      Playwright E2E tests
├── infra/postgres/           PostgreSQL init SQL (extensions)
├── scripts/                  Utility scripts (VAPID key generation)
├── specs/                    Engineering specification documents
├── docker-compose.infra.yml  PostgreSQL + MinIO
└── docker-compose.app.yml    API + Frontend
```

## CI

GitHub Actions runs four jobs on every push:

1. **backend-unit** — Gradle test (unit tests only)
2. **backend-integration** — Testcontainers integration tests (PostgreSQL + pgvector)
3. **frontend** — TypeScript check + Vitest + Vite build
4. **e2e** — Playwright tests against running backend + frontend
5. **docker-build** — Multi-stage Docker builds (main branch only)
