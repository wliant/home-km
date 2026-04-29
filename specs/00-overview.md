# Home KM — System Overview

## 1. Purpose and Audience

This document is the entry point for the Home KM engineering specification set. Every subsequent spec is self-contained but cross-references this overview and each other where required.

**Audience:**
- **Backend engineers** — Spring Boot API, PostgreSQL, MinIO integration
- **Frontend engineers** — React PWA, Service Worker, offline sync
- **DevOps / infrastructure** — Docker Compose, environment configuration
- **QA / test engineers** — unit, integration, and E2E test strategy

---

## 2. System Summary

Home KM is a self-hosted household knowledge management Progressive Web App (PWA). It gives a family or small household a shared digital brain: a notebook for notes of all kinds, and a file vault for documents and media.

**Two primary content domains:**

| Domain | Description |
|--------|-------------|
| **Notebook** | Freeform notes with labels, checklists, reminders, and tags. Notes live in folders alongside files. |
| **File Vault** | Documents and images stored in MinIO, organised in the same folder tree as notes. Supports offline upload queuing for poor-connectivity scenarios (e.g. travel). |

**Target scale:** 2–6 household members. Thousands of files. Not designed for multi-tenant or enterprise use.

**Recovery objectives** (sized for the target scale):
- **RTO 4 hours** — acceptable downtime after a hardware failure or restore.
- **RPO 1 hour** — maximum data loss tolerated. Backup cadence (`docs/backups.md`) and the monthly restore drill (`docs/restore-drill.md`) must keep this achievable.

Service-Level Objectives for availability, latency, and push delivery are documented in `docs/slo.md` and surface as Prometheus alerts (see `docker-compose.observability.yml`).

---

## 3. App Name Configurability

The application name is driven by the `APP_NAME` environment variable (default: `Home KM`). This string must appear in:

- The browser tab title (`<title>`)
- The PWA `manifest.json` fields `name` and `short_name`
- Web Push notification `title` field
- Email subjects (if email is added in future)

`APP_THEME_COLOR` (default: `#6366f1`) drives the PWA `theme_color` and the primary UI accent colour.

Both values are injected at frontend **build time** via Vite's `define` / `import.meta.env` mechanism. They are not runtime-configurable without a rebuild.

---

## 4. Technology Choices

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend | React + TypeScript, Vite | React 18, Vite 5 |
| Styling | Tailwind CSS | v3 |
| State | Zustand (auth), TanStack Query v5 (server) | — |
| Forms | React Hook Form + Zod | — |
| PWA | vite-plugin-pwa (Workbox) | — |
| Backend | Spring Boot + Gradle (Kotlin DSL) | Spring Boot 3.x, JDK 21 |
| Database | PostgreSQL + pgvector + pg_trgm | PostgreSQL 15 |
| File storage | MinIO | latest stable |
| Migrations | Flyway | — |
| Frontend container | nginx | alpine |
| Runtime | Docker Compose | — |

No substitutions are in scope for v1.

---

## 5. Spec Navigation Map

| Spec | Feature Area | Depends On |
|------|-------------|------------|
| `00-overview.md` | System context (this file) | — |
| `01-data-model.md` | Database schema | — |
| `02-auth.md` | Authentication, user management | 01 |
| `03-folders.md` | Folder tree CRUD | 01, 09, 11 |
| `04-notes.md` | Note CRUD, checklists | 01, 09, 11 |
| `05-reminders.md` | Reminders, Web Push delivery | 01, 04, 11 |
| `06-files.md` | File upload, MinIO, thumbnails | 01, 09, 10, 11 |
| `07-tags.md` | Tag registry, polymorphic tagging | 01, 11 |
| `08-search.md` | Full-text search, filters | 01, 09, 11 |
| `09-child-safe.md` | Child-safe rules, access control | 01 |
| `10-offline-pwa.md` | Service Worker, IndexedDB, push | — |
| `11-api-conventions.md` | HTTP conventions, error envelope | — |
| `12-infrastructure.md` | Docker Compose, .env, nginx | all |
| `13-testing.md` | Test strategy, E2E matrix | all |
| `14-frontend-architecture.md` | React structure, routing, state | all |

**Critical reading order for a new engineer:** `11` → `01` → `09` → `02` → feature specs → `12` → `13` → `14`.

---

## 6. Decision Log

All architectural decisions recorded here so implementors understand the rationale.

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | Notes and files share the same folder tree | Unified navigation; a "Japan 2025" folder holds both travel notes and photos |
| D2 | Notes body stored as raw Markdown | No server-side parsing needed; frontend renders with `react-markdown` |
| D3 | Files served via MinIO presigned URLs | Spring Boot never proxies large binaries; presigned URLs expire after `PRESIGNED_URL_EXPIRY_MINUTES` |
| D4 | pgvector column added but not indexed in v1 | Reserves the schema for future semantic search without blocking v1 delivery |
| D5 | Embedding dimension: 1536 | OpenAI `text-embedding-3-small` / `text-embedding-ada-002` compatible |
| D6 | `pg_trgm` extension added alongside `pgvector` | Required for efficient `ts_headline()` and trigram-based tag autocomplete |
| D7 | JWT is stateless; no token blacklist in v1 | Household scale makes token theft unlikely; simplifies backend significantly |
| D8 | First registered user becomes admin | Bootstraps the household without a separate setup step |
| D9 | Child accounts can create/edit their own content | Children should be able to add items to shopping lists and write their own notes |
| D10 | Content created by children is automatically child-safe | A child cannot create content that other children cannot see |
| D11 | Children cannot delete anything | Prevent accidental data loss; children can edit but not destroy |
| D12 | Any adult can delete any content | Household trust model; no per-item ownership lock on deletion |
| D13 | Reminder recipients are configurable per reminder | A shopping list reminder may need to notify one person; a family event reminder may need all adults |
| D14 | Folder delete with `?force=true` recursively deletes contents | Convenience for bulk cleanup; without the flag, `409` protects against accidental deletion |
| D15 | File binary replacement via `PUT /api/files/{id}/content` | Useful when correcting a scan or updating a document without losing metadata and tags |
| D16 | Thumbnail always output as JPEG | Simplifies thumbnail rendering; consistent format regardless of source |
| D17 | Search excerpt from `body` (notes), `description` (files/folders) | Body is the primary content for notes; description is the best summary for files |
| D18 | Tag color is a free hex input | No artificial palette constraint; household can use any colour scheme |
| D19 | Two Docker Compose files: infra and app | Allows infra (DB, MinIO) to run independently; useful for development and upgrades |
| D20 | Note at root level (`folder_id = NULL`) is allowed | Not every note belongs in a folder; virtual root is a valid location |
