# Home KM — Implementation Plan

> Status legend: `[ ]` Not started · `[~]` In progress · `[x]` Done · `[!]` Blocked

---

## Phase 1 — Project Scaffolding

### 1.1 Backend (Spring Boot)
- [x] Initialize Gradle project with Kotlin DSL (`settings.gradle.kts`, `build.gradle.kts`)
- [x] Add dependencies: Spring Web, Spring Security, Spring Data JPA, Flyway, PostgreSQL driver, MinIO SDK, jjwt, Spring Scheduler
- [x] Configure multi-stage `Dockerfile` (gradle:8-jdk21-alpine → eclipse-temurin:21-jre-alpine)
- [x] Set up `application.yml` with environment variable bindings for all 34 `.env` keys
- [x] Create base package structure (`auth`, `folder`, `note`, `file`, `tag`, `search`, `reminder`, `push`, `admin`, `common`)

### 1.2 Frontend (React + Vite)
- [x] Scaffold Vite project with React 18 + TypeScript template
- [x] Install dependencies: Tailwind CSS, Zustand, TanStack Query, React Hook Form, Zod, Axios, react-markdown, remark-gfm, vite-plugin-pwa, Workbox
- [x] Configure `vite.config.ts` with `define` plugin (APP_NAME, APP_THEME_COLOR) and PWA plugin
- [x] Set up `tailwind.config.ts` with custom theme tokens
- [x] Create directory structure: `src/{api,components,features,lib,types}`
- [x] Configure multi-stage `Dockerfile` (node:20-alpine → nginx:alpine) with `envsubst`

### 1.3 Infrastructure
- [x] Write `docker-compose.infra.yml` (PostgreSQL pgvector:pg15, MinIO RELEASE.2024-01-16)
- [x] Write `docker-compose.app.yml` (Spring Boot API, nginx frontend)
- [x] Write `init.sql` to enable `pgvector` and `pg_trgm` extensions
- [x] Write `.env.example` with all 34 variables and documentation comments
- [x] Write VAPID key generation script (`scripts/gen-vapid-keys.sh`)
- [x] Write `nginx.conf.template` with SPA fallback, proxy to API, configurable `client_max_body_size`

---

## Phase 2 — Database Migrations (Flyway)

- [x] `V001__init.sql` — all 11 tables in dependency order (users, folders, notes, checklist_items, reminders, reminder_recipients, files, tags, taggings, push_subscriptions)
- [x] All indexes: GIN on search_vector columns, trgm GIN on tags.name, partial index on reminders, unique functional index on tags (LOWER(name))
- [x] Three tsvector trigger functions + triggers (folders, notes, files) — BEFORE INSERT OR UPDATE

---

## Phase 3 — Backend: Common Infrastructure

### 3.1 Security & Auth Utilities
- [x] `JwtService` — HS256 token generation, validation, expiry parsing
- [x] `JwtAuthFilter` — Spring Security filter to extract and validate Bearer token; distinguishes TOKEN_EXPIRED
- [x] `SecurityConfig` — permit `/api/auth/register` and `/api/auth/login`; require auth for all other `/api/**`
- [x] `UserPrincipal` — Spring Security `UserDetails` wrapper with role and userId
- [x] Password validation: 8+ chars, uppercase, lowercase, digit (Bean Validation regex)

### 3.2 API Conventions
- [x] `ErrorResponse` — code, message, timestamp, optional field errors
- [x] `GlobalExceptionHandler` — `@ControllerAdvice` mapping domain exceptions to HTTP codes
- [x] `PageResponse<T>` — pagination wrapper (content, page, size, totalElements, totalPages, first, last)
- [x] `CorsConfig` — configurable via `CORS_ALLOWED_ORIGINS` env var
- [x] `AppProperties` — `@ConfigurationProperties("app")` binding all env vars

### 3.3 Child-Safe Service
- [x] `ChildSafeService.cascadeMarkSafe(folderId)` — mark folder + all descendants + their notes/files safe (single transaction)
- [x] `ChildSafeService.demoteFolderOnUnsafeItem(folderId)` — demote folder when unsafe note/file added
- [x] `ChildSafeService.applyMoveRules(item, destinationFolder)` — item becomes safe if destination is safe
- [x] `ChildSafeService` — enforce read filter: child users only see `is_child_safe=true` items
- [x] `ChildSafeService` — silent override: content created by child forced to `is_child_safe=true`

---

## Phase 4 — Backend: Feature Modules

### 4.1 Authentication (`/api/auth`)
- [x] `POST /api/auth/register` — create user; first user becomes admin
- [x] `POST /api/auth/login` — validate credentials, return JWT; constant-time comparison for unknown emails
- [x] `GET /api/auth/me` — return current user profile
- [x] `PUT /api/auth/me` — update display name and/or password

### 4.2 Admin User Management (`/api/admin/users`)
- [x] `GET /api/admin/users` — user list (admin only)
- [x] `POST /api/admin/users` — create user (admin only)
- [x] `PUT /api/admin/users/{id}` — update role/status (admin only; cannot remove own admin flag)
- [x] `DELETE /api/admin/users/{id}` — soft-delete user (cannot delete self)
- [x] `POST /api/admin/users/{id}/reset-password` — admin-triggered password reset

### 4.3 Folders (`/api/folders`)
- [x] `GET /api/folders` — return full folder tree (recursive CTE, depth-limited to 20)
- [x] `GET /api/folders/{id}` — folder detail with child-safe status
- [x] `POST /api/folders` — create folder (sibling name uniqueness check, cycle prevention)
- [x] `PUT /api/folders/{id}` — update name and/or parent (move); run cycle-prevention CTE on move
- [x] `DELETE /api/folders/{id}` — delete leaf folder; `409` if has children without `?force=true`
- [x] `DELETE /api/folders/{id}?force=true` — recursive delete of all descendants, notes, files (MinIO failures logged)
- [x] `PUT /api/folders/{id}/child-safe` — toggle `is_child_safe`; trigger cascade via `ChildSafeService`

### 4.4 Notes (`/api/notes`, `/api/folders/{id}/notes`)
- [x] `GET /api/notes` — paginated list (summary: no body, includes counts); honour child-safe filter
- [x] `GET /api/folders/{folderId}/notes` — scoped list
- [x] `GET /api/notes/{id}` — full detail with checklistItems and reminders
- [x] `POST /api/notes` — create note; child: force child-safe; demote folder if unsafe
- [x] `PUT /api/notes/{id}` — update note (adults only can toggle child-safe); apply move rules if folder changes
- [x] `DELETE /api/notes/{id}` — adult-only delete; `403` for child accounts

### 4.5 Checklist Items (`/api/notes/{noteId}/checklist-items`)
- [x] `GET /api/notes/{noteId}/checklist-items` — ordered list
- [x] `POST /api/notes/{noteId}/checklist-items` — add item (max 500 validation)
- [x] `PUT /api/notes/{noteId}/checklist-items/{id}` — update text/checked/sort_order
- [x] `DELETE /api/notes/{noteId}/checklist-items/{id}` — remove item
- [x] `PUT /api/notes/{noteId}/checklist-items/reorder` — bulk sort_order update

### 4.6 Reminders (`/api/notes/{noteId}/reminders`)
- [x] `GET /api/notes/{noteId}/reminders` — list reminders for note
- [x] `POST /api/notes/{noteId}/reminders` — create reminder (max 10, remindAt in future, max 5 years ahead)
- [x] `PUT /api/notes/{noteId}/reminders/{id}` — update reminder
- [x] `DELETE /api/notes/{noteId}/reminders/{id}` — delete reminder
- [x] `ReminderScheduler` — `@Scheduled(fixedRate=60000)`, finds due reminders, sends Web Push, advances recurrence
- [x] Web Push delivery — VAPID signing, base64url key parsing, push to all recipients' subscriptions

### 4.7 Files (`/api/files`, `/api/folders/{id}/files`)
- [x] `POST /api/files` — multipart upload; insert pending row; upload to MinIO; update row; trigger async thumbnail
- [x] Idempotency via `clientUploadId` — return existing record if duplicate
- [x] MinIO key scheme: `/{userId}/{folderSegment}/{fileId}/{filename}`
- [x] Async thumbnail generation — image files only, max 256×256 JPEG, stored at `{key}_thumb.jpg`
- [x] `GET /api/files/{id}` — return metadata + presigned download URL (15min expiry)
- [x] `PUT /api/files/{id}` — update metadata (name, folder, child-safe)
- [x] `DELETE /api/files/{id}` — delete DB row and MinIO object; adult only
- [ ] `GET /api/files/{id}/thumbnail` — presigned URL for thumbnail (via toResponse presigned URL)
- [x] `PUT /api/files/{id}/content` — replace binary; upload new version to MinIO; update key

### 4.8 Tags (`/api/tags`)
- [x] `GET /api/tags` — list all tags (global, not user-scoped)
- [x] `GET /api/tags?q=...` — trigram autocomplete (max 10, case-insensitive)
- [x] `POST /api/tags` — create tag (name + hex color); adults only
- [x] `PUT /api/tags/{id}` — update tag; adults only
- [x] `DELETE /api/tags/{id}` — delete tag and all taggings; adults only
- [x] `POST /api/notes/{id}/tags` — attach tags (max 20 per entity)
- [x] `DELETE /api/notes/{id}/tags/{tagId}` — detach tag
- [x] `POST /api/files/{id}/tags` — attach tags to file
- [x] `DELETE /api/files/{id}/tags/{tagId}` — detach tag from file
- [x] `POST /api/folders/{id}/tags` — attach tags to folder
- [x] `DELETE /api/folders/{id}/tags/{tagId}` — detach tag from folder

### 4.9 Search (`/api/search`)
- [x] `GET /api/search?q=&types=&folderId=&tagIds=&page=&size=` — unified search
- [x] Full-text search using `plainto_tsquery` (safe from injection)
- [x] Ranked results via `ts_rank_cd`
- [x] Excerpt generation via `ts_headline` with `<b>` highlighting
- [x] Folder-scope filter using recursive CTE (max 20 levels)
- [x] Tag filter using `HAVING COUNT(DISTINCT tag_id) = tagCount` (AND semantics)
- [x] Type filter: omit irrelevant UNION branches when types param is set
- [x] Child-safe filter applied to all branches for child users

### 4.10 Push Subscriptions (`/api/push`)
- [x] `GET /api/push/vapid-public-key` — return VAPID public key for browser subscription
- [x] `POST /api/push/subscribe` — save PushSubscription (endpoint, p256dh, auth) for current user
- [x] `DELETE /api/push/subscribe` — remove subscription by endpoint

### 4.11 Test Trigger (test profile only)
- [x] `POST /api/test/trigger-scheduler` — `@Profile("test")` controller that fires `ReminderScheduler`

---

## Phase 5 — Frontend: Core Infrastructure

### 5.1 API Client
- [x] `src/api/client.ts` — Axios instance with base URL, JWT `Authorization` interceptor
- [x] Token expiry check: on `401 TOKEN_EXPIRED` clear auth state and redirect to `/login`
- [x] `src/api/authApi.ts` — typed auth API module

### 5.2 State Management
- [x] `src/lib/authStore.ts` — Zustand store: `token`, `user`, `isAuthenticated`; persisted to localStorage with expiry validation
- [x] TanStack Query provider in `main.tsx` with `queryClient.ts`

### 5.3 Routing
- [x] React Router setup: `/login`, `/register`, `/` (home)
- [x] `ProtectedRoute` wrapper — redirect to `/login` if unauthenticated
- [x] Full 10-route setup: `/folders/:id`, `/notes/:id`, `/notes/new`, `/notes/:id/edit`, `/files`, `/search`, `/admin/users`, `/settings`
- [x] `AdminRoute` wrapper — redirect to `/` if not admin

### 5.4 Layout
- [x] `AppLayout` — responsive shell: left sidebar on desktop (≥768px), bottom tab bar on mobile
- [x] `Sidebar` — folder tree navigation, search link, admin link (admin users only)
- [x] `BottomTabBar` — Home, Search, Upload, Settings tabs
- [x] `OfflineBanner` — shown when `navigator.onLine === false`
- [x] Child-safe "Kid Mode" badge — muted theme banner, hide edit/delete/admin controls

---

## Phase 6 — Frontend: Feature Modules

### 6.1 Auth Feature (`src/features/auth`)
- [x] `LoginPage` — email + password form, React Hook Form + Zod validation, error display
- [x] `RegisterPage` — email + display name + password, password strength validation
- [x] `SettingsPage` — update display name and/or password

### 6.2 Folder Feature (`src/features/folders`)
- [x] `FolderPage` — shows sub-folders, notes list, files list for a folder; create subfolder inline; delete
- [x] Rename / move folder modal
- [x] Child-safe toggle with cascade warning

### 6.3 Notes Feature (`src/features/notes`)
- [x] `NotesListPage` — paginated summary cards with label badge, checklist count
- [x] `NoteDetailPage` — full note with Markdown body, checklist (toggle/add/delete), shopping list mode, reminders, tags
- [x] `NoteEditorPage` — title + Markdown body textarea, label selector, folder picker, child-safe toggle; create/edit flow
- [x] `RemindersSection` — list reminders, create/edit/delete reminder with recurrence picker (daily/weekly/monthly/yearly)
- [x] `TagAutocomplete` + `TagChip` on NoteDetailPage — attach/detach tags with trigram autocomplete and create-on-fly

### 6.4 Files Feature (`src/features/files`)
- [x] `FilesListPage` — grid view of files; thumbnail preview for images; upload button; delete; link to detail
- [x] `FileDetailPage` — metadata, presigned download link, tags section, delete
- [x] Upload progress indicator — per-file progress bar via axios onUploadProgress

### 6.5 Search Feature (`src/features/search`)
- [x] `SearchPage` — query input, results with highlighted excerpts, links to type-appropriate detail pages

### 6.6 Tags Feature (`src/features/tags`)
- [x] `TagAutocomplete` component — trigram-based typeahead, create-on-the-fly option, attach/detach
- [x] `TagChip` component — colored badge with remove button
- [x] Tag manager page (admin only) — list, create, edit, delete global tags

### 6.7 Admin Feature (`src/features/admin`)
- [x] `AdminUsersPage` — user table with role/status badges; create user form; reset password; delete user
- [x] `AdminRoute` guard — redirects non-admin users to `/`

---

## Phase 7 — PWA & Offline Support

### 7.1 Service Worker
- [x] `vite-plugin-pwa` + custom `src/sw.ts` (injectManifest strategy): Workbox precache + NetworkFirst for `/api/**`
- [x] Service Worker push event handler — show notification with note title, body, icon
- [x] Service Worker `notificationclick` handler — focus/open note URL on click
- [x] Background Sync API registration — skipped (browser support limited); `online` event fallback used instead

### 7.2 IndexedDB Offline Queue
- [x] `src/lib/offlineDb.ts` — `homekm-offline-queue` DB, `upload-queue` store with status index
- [x] `useOfflineQueue` hook — polls every 5s + `online` event, processes pending, exposes stats
- [x] `QueueStatusBadge` component — shows pending/failed count in sidebar; retry button for failed items
- [x] File upload path — detect `!navigator.onLine` → `enqueueUpload()` in FilesListPage

### 7.3 Push Notifications
- [x] `usePushSubscription` hook — fetches VAPID key, `pushManager.subscribe()`, POST/DELETE `/api/push/subscribe`
- [x] Settings page toggle — enable/disable push notifications, shows "Enabled on this device" state
- [x] iOS "Add to Home Screen" prompt — `IOSInstallPrompt` component, dismissible via sessionStorage

---

## Phase 8 — Testing

### 8.1 Backend Unit Tests
- [x] `JwtServiceTest` — token generation, validation, expiry, claim assertions (7 tests)
- [x] `ChildSafeServiceTest` — CS-1 to CS-5 cascade scenarios via Mockito (9 tests)
- [x] `ReminderSchedulerTest` — recurrence advancement (daily/weekly/monthly/yearly), push delivery, idempotency (7 tests)
- [x] `UserServiceTest` — covered by `AuthIntegrationTest` (register first user = admin, duplicate email 409)

### 8.2 Backend Integration Tests (Testcontainers + pgvector:pg15)
- [x] Auth flow — register (first user = admin), duplicate email 409, login, wrong password 401, getMe, updateMe
- [x] Note CRUD — create 201, get 200, update 200, delete 204, double-delete 404, missing title 400, unauth 401
- [x] Child-safe cascades — mark safe/unsafe, item moves, child account access filter (4 tests)
- [ ] File upload — requires MinIO (mocked in current tests)
- [x] Full-text search — ranking, excerpt highlighting (5 tests)

### 8.3 Frontend Unit Tests (Vitest + RTL)
- [x] `TagChip` — renders name, applies color, shows/hides remove button, fires onRemove (6 tests)
- [x] `QueueStatusBadge` — empty renders nothing, pending count, failed count + retry, retry handler (5 tests)
- [x] `authStore` — initial state, setAuth, clearAuth, admin flag preservation (4 tests)
- [x] `TagAutocomplete` — filters suggestions, create-on-fly, read-only mode (6 tests)

### 8.4 E2E Tests (Playwright)
- [x] `test_auth` — register, login, logout, protected route redirect (4 tests)
- [x] `test_notes` — create note, edit, delete (3 tests)
- [x] `test_search` — query returns results, no-match empty state (2 tests)

---

## Phase 9 — CI / CD Pipeline

- [x] `.github/workflows/ci.yml` — 4 jobs: backend-unit, backend-integration (Testcontainers), frontend (typecheck + test + build), docker-build (main branch only)
- [x] Docker build uses `docker/build-push-action@v5` with GitHub Actions cache (`type=gha`)
- [x] `gradle/actions/setup-gradle@v3` bootstraps Gradle 8.6 in CI without wrapper JAR
- [x] Stage 3 — E2E tests with Playwright (e2e/ project, runs on `main` branch only)
- [ ] Publish Docker images to a registry (ghcr.io or Docker Hub) — optional, not implemented

---

## Completion Summary

| Phase | Description | Status |
|---|---|---|
| 1 | Project Scaffolding | `[x]` Done |
| 2 | Database Migrations | `[x]` Done |
| 3 | Backend Common Infrastructure | `[x]` Done |
| 4 | Backend Feature Modules | `[x]` Done |
| 5 | Frontend Core Infrastructure | `[x]` Done |
| 6 | Frontend Feature Modules | `[x]` Done (tag manager, folder rename/move, child-safe toggle all done) |
| 7 | PWA & Offline Support | `[x]` Done |
| 8 | Testing | `[x]` Done (22 frontend unit, 20+ backend unit/integration, 9 E2E) |
| 9 | CI / CD Pipeline | `[x]` Done (5-job workflow: unit, integration, frontend, e2e, docker-build) |
