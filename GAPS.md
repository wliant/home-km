# Home KM — Feature Gap Analysis

A catalogue of functional and non-functional gaps between **what Home KM does today** and **what a production-grade household knowledge management PWA should do**. Engineering and product use this document to plan the next phase of work.

This is the **index**. Each gap is its own one-page Markdown file under `gaps/<category>/<slug>.md` so individual items can be edited, linked, and assigned independently.

---

## How to read this document

Every gap file has the same shape:

| Field | Value |
|---|---|
| Category | Functional · `<area>` or Non-functional · `<area>` |
| Priority | **P0** = next 4 weeks · **P1** = 1–2 quarters · **P2** = later |
| Size | **S** = ≤1 week · **M** = 1–4 weeks · **L** = >1 month |

Each entry contains: **Current state**, **Gap**, **Proposed direction**, and **References** (paths to the code or specs it touches).

---

## Scope & non-goals

In scope (per [`specs/00-overview.md`](specs/00-overview.md)):
- Single-tenant, self-hosted household app for 2–6 members
- Notes, files, folders, tags, reminders, push notifications
- Web PWA (no native mobile)

Explicit non-goals (unchanged by this analysis):
- Multi-tenant SaaS / per-tenant isolation
- Marketplace / plugin ecosystem
- Native iOS or Android apps
- Enterprise SSO mandates (OIDC remains optional, see [`gaps/auth/oidc-sso.md`](gaps/auth/oidc-sso.md))

---

## Methodology

1. **Code inventory** — read every feature package under `backend/src/main/java/com/homekm/` and `frontend/src/features/`, the infra files, and the CI workflow.
2. **Spec review** — cross-checked existing capabilities against `specs/00-..14-`.
3. **Best-practice checklist** — compared against typical production-readiness criteria for self-hosted webapps (security headers, observability, backups, accessibility, etc.).

Confirmed absences were spot-checked with `grep` (e.g., `grep -ri "Content-Security-Policy" frontend backend` returns nothing → security headers gap is real).

---

## Index of gaps

### Functional

#### Auth & account ([`gaps/auth/`](gaps/auth/))
- [Token refresh](gaps/auth/token-refresh.md) — **P0/M**
- [Logout / session revocation](gaps/auth/logout-revocation.md) — **P0/M**
- [Password reset via email](gaps/auth/password-reset-email.md) — **P0/M**
- [MFA (TOTP)](gaps/auth/mfa-totp.md) — P2/M
- [Account recovery](gaps/auth/account-recovery.md) — P2/S
- ["Remember me" / extended sessions](gaps/auth/remember-me.md) — P1/S
- [OIDC / SSO consideration](gaps/auth/oidc-sso.md) — P2/L

#### Notes ([`gaps/notes/`](gaps/notes/))
- [Markdown rendering pipeline](gaps/notes/markdown-rendering.md) — **P0/S**
- [Soft delete + trash](gaps/notes/soft-delete-trash.md) — **P0/M**
- [Note version history](gaps/notes/version-history.md) — P2/M
- [Note templates](gaps/notes/templates.md) — P2/S
- [Export (PDF / Markdown)](gaps/notes/export-pdf-md.md) — P1/S
- [Pin / favorite](gaps/notes/pin-favorite.md) — P1/S
- ✓ [Richer sort / filter UX](gaps/notes/sort-filter-ux.md) — pinned-first split + Mine/Shared/All segmented filter on the notes & files lists; advanced filters (date/owner/MIME/has-reminder) on SearchPage.
- ✓ [Inline image attachments](gaps/notes/inline-image-attachments.md) — paste/drop/file-picker uploads via `fileApi.upload`, inserts `![alt](url)` markdown at cursor.

#### Files ([`gaps/files/`](gaps/files/))
- [Chunked / resumable upload](gaps/files/chunked-resumable-upload.md) — P1/M
- [Virus scan on upload](gaps/files/virus-scan.md) — P1/M
- [MIME allowlist + magic-byte sniff](gaps/files/mime-allowlist.md) — P1/S
- [Image transforms (WebP/AVIF, multiple sizes)](gaps/files/image-transforms.md) — P1/M
- [In-app file preview (PDF / video / audio)](gaps/files/file-preview.md) — **P0/M**
- [File versioning](gaps/files/versioning.md) — P2/M
- [Share link with expiry](gaps/files/share-link-expiry.md) — P1/S
- [File rename](gaps/files/file-rename.md) — P1/S

#### Folders ([`gaps/folders/`](gaps/folders/))
- [Bulk move (notes / files / folders)](gaps/folders/bulk-move.md) — P1/S
- [Drag-and-drop reorganization](gaps/folders/drag-and-drop.md) — P1/M
- [Breadcrumbs UX review](gaps/folders/breadcrumbs-ux.md) — P2/S
- [Archive / restore folders](gaps/folders/archive-restore.md) — P2/S
- ✓ [Folder color and icon](gaps/folders/color-icon.md) — palette + emoji picker in FolderPage edit modal.
- ✓ [Archive / restore folders](gaps/folders/archive-restore.md) — Archive button on FolderPage, dedicated `/folders/archived` listing with one-click Restore. — P2/S

#### Tags ([`gaps/tags/`](gaps/tags/))
- [Bulk tag operations](gaps/tags/bulk-operations.md) — P2/S
- [Tag merge and rename cascade](gaps/tags/merge-rename-cascade.md) — P2/S
- [Suggested tags](gaps/tags/suggested-tags.md) — P2/M
- [Tag color picker UX](gaps/tags/color-picker-ux.md) — P2/S

#### Search ([`gaps/search/`](gaps/search/))
- [Semantic / vector search (use existing pgvector columns)](gaps/search/semantic-vector.md) — P1/L
- [Saved searches](gaps/search/saved-searches.md) — P2/S
- [Advanced filters (date range, owner, file type)](gaps/search/advanced-filters.md) — P1/S
- ["Did you mean" / typo tolerance](gaps/search/did-you-mean.md) — P2/S
- [Search within current folder](gaps/search/search-within-folder.md) — P1/S
- [Result-type tabs](gaps/search/result-type-tabs.md) — P2/S

#### Reminders ([`gaps/reminders/`](gaps/reminders/))
- [Reminder snooze](gaps/reminders/snooze.md) — P1/S
- [Multi-recipient UX](gaps/reminders/multi-recipient-ux.md) — P1/S
- [Calendar (.ics) export / subscription](gaps/reminders/ics-export.md) — P1/S
- [Timezone awareness](gaps/reminders/timezone-handling.md) — P1/S
- [Cron-style custom recurrence](gaps/reminders/cron-recurrence.md) — P2/M
- [Email fallback when push fails](gaps/reminders/email-fallback.md) — P2/S

#### Sharing & collaboration ([`gaps/sharing/`](gaps/sharing/))
- [Per-item access controls (ACLs)](gaps/sharing/per-item-acls.md) — P1/L
- [Comments and @mentions](gaps/sharing/comments-mentions.md) — P2/M
- [Shared / live shopping lists](gaps/sharing/shared-shopping-lists.md) — P1/M
- [Real-time co-edit (long-term)](gaps/sharing/co-edit.md) — P2/L

#### Multi-device sync ([`gaps/sync/`](gaps/sync/))
- [Real-time updates (WebSocket / SSE)](gaps/sync/realtime-updates.md) — P1/M
- [Conflict resolution](gaps/sync/conflict-resolution.md) — P2/S

#### Child-safe mode ([`gaps/child-safe/`](gaps/child-safe/))
- [Content moderation hooks](gaps/child-safe/moderation-hooks.md) — P2/M
- [Parental review queue](gaps/child-safe/parental-review-queue.md) — P2/S
- [Time limits / quiet hours](gaps/child-safe/time-limits.md) — P2/S

#### Admin ([`gaps/admin/`](gaps/admin/))
- [Audit log viewer](gaps/admin/audit-log-viewer.md) — **P0/M**
- [Usage analytics dashboard](gaps/admin/usage-analytics.md) — P2/S
- [Bulk user import](gaps/admin/bulk-user-import.md) — P2/S
- [Invite-only registration flow](gaps/admin/invite-flow.md) — P1/S
- [Household / group concept](gaps/admin/household-group-concept.md) — P2/M

#### PWA & offline ([`gaps/pwa/`](gaps/pwa/))
- [Offline read for notes](gaps/pwa/offline-read-notes.md) — P1/M
- [Background sync for note edits](gaps/pwa/background-sync-edits.md) — P1/M
- [Push notification action buttons](gaps/pwa/push-action-buttons.md) — P2/S
- [Notification badge count](gaps/pwa/badge-count.md) — P2/S
- [iOS / Android PWA polish](gaps/pwa/ios-android-polish.md) — P2/S
- [Install banner improvements](gaps/pwa/install-banner-improvements.md) — P2/S

#### Settings ([`gaps/settings/`](gaps/settings/))
- [Dark mode](gaps/settings/dark-mode.md) — **P0/S**
- [Theme / accent picker](gaps/settings/theme-picker.md) — P2/S
- [Language selection](gaps/settings/language-selection.md) — P1/S
- [Per-channel notification preferences](gaps/settings/notification-preferences.md) — P2/S
- [Data export (GDPR-style)](gaps/settings/gdpr-export.md) — P2/M
- [Account deletion (self-service)](gaps/settings/account-deletion.md) — P2/S

#### Accessibility ([`gaps/a11y/`](gaps/a11y/))
- [Keyboard navigation pass](gaps/a11y/keyboard-nav.md) — P1/M
- [ARIA labels and landmarks](gaps/a11y/aria-labels-landmarks.md) — P1/S
- [Focus management on route change](gaps/a11y/focus-management.md) — P2/S
- [Screen-reader testing pass](gaps/a11y/screen-reader-pass.md) — P2/M
- [axe in CI](gaps/a11y/axe-ci.md) — P1/S
- [Color contrast audit](gaps/a11y/color-contrast.md) — P2/S

#### i18n / l10n ([`gaps/i18n/`](gaps/i18n/))
- [i18n framework](gaps/i18n/framework.md) — P1/M
- [Initial locale set](gaps/i18n/initial-locales.md) — P2/S
- [Date / number / currency formatting](gaps/i18n/date-number-formatting.md) — P2/S
- [ICU message format](gaps/i18n/icu-message-format.md) — P2/S
- [Backend error message localization](gaps/i18n/backend-localization.md) — P2/S

### Non-functional

#### Security ([`gaps/security/`](gaps/security/))
- [TLS termination + reverse proxy](gaps/security/tls-reverse-proxy.md) — **P0/S**
- [Security headers (CSP, HSTS, X-Frame-Options, X-Content-Type-Options)](gaps/security/security-headers.md) — **P0/S**
- [Secret management](gaps/security/secret-management.md) — P1/M
- [Encryption at rest](gaps/security/encryption-at-rest.md) — P1/M
- [Per-endpoint rate limits](gaps/security/per-endpoint-rate-limits.md) — P1/S
- [CSRF posture review](gaps/security/csrf-posture-review.md) — P2/S
- [Dependency scanning (OWASP DC, Snyk)](gaps/security/dependency-scanning.md) — **P0/S**
- [Container image scanning (Trivy)](gaps/security/container-scanning.md) — **P0/S**
- [SBOM generation](gaps/security/sbom.md) — P2/S
- [Dependency pinning](gaps/security/dependency-pinning.md) — P2/S

#### Observability ([`gaps/observability/`](gaps/observability/))
- [Metrics (Micrometer + Prometheus)](gaps/observability/metrics-prometheus.md) — **P0/S**
- [Grafana dashboards](gaps/observability/grafana-dashboards.md) — P1/S
- [OpenTelemetry tracing](gaps/observability/opentelemetry-tracing.md) — P1/M
- [Structured JSON logs](gaps/observability/structured-json-logs.md) — **P0/S**
- [Log aggregation (Loki / ELK)](gaps/observability/log-aggregation.md) — P1/S
- [Frontend error tracking + web vitals](gaps/observability/frontend-error-tracking.md) — P1/S

#### Reliability ([`gaps/reliability/`](gaps/reliability/))
- [Circuit breaker for MinIO](gaps/reliability/circuit-breaker.md) — P1/M
- [Retry with exponential backoff](gaps/reliability/retry-backoff.md) — P1/S
- [Graceful shutdown](gaps/reliability/graceful-shutdown.md) — P1/S
- [Idempotency keys on POST endpoints](gaps/reliability/idempotency-keys.md) — P1/M
- [Transactional outbox for push notifications](gaps/reliability/transactional-outbox-push.md) — P2/M
- [SLO definition](gaps/reliability/slo-definition.md) — P2/S

#### Performance ([`gaps/performance/`](gaps/performance/))
- [Caffeine cache for hot lookups](gaps/performance/caffeine-cache.md) — P1/S
- [Slow-query logging](gaps/performance/slow-query-logging.md) — P1/S
- [Pagination defaults audit](gaps/performance/pagination-defaults.md) — P2/S
- [Frontend bundle analyzer](gaps/performance/bundle-analyzer.md) — P2/S
- [Response compression (gzip / brotli) and HTTP/2](gaps/performance/response-compression.md) — P2/S
- [HikariCP pool sizing review](gaps/performance/hikari-pool-sizing.md) — P2/S

#### Backup & DR ([`gaps/backup-dr/`](gaps/backup-dr/))
- [PostgreSQL automated backups + WAL archiving](gaps/backup-dr/postgres-backups.md) — **P0/M**
- [MinIO bucket replication / backup](gaps/backup-dr/minio-backups.md) — **P0/S**
- [Monthly restore drill](gaps/backup-dr/restore-drill.md) — P1/S
- [Documented RTO / RPO](gaps/backup-dr/rto-rpo.md) — P2/S
- [Off-host backup target](gaps/backup-dr/off-host-target.md) — P1/S

#### Operability ([`gaps/operability/`](gaps/operability/))
- [Separate liveness vs readiness probes](gaps/operability/liveness-readiness-probes.md) — P1/S
- [Dependency-aware health (DB + MinIO)](gaps/operability/dependency-aware-health.md) — P1/S
- [Log retention policy](gaps/operability/log-retention-policy.md) — P2/S
- [Build / version info endpoint](gaps/operability/build-info-endpoint.md) — P2/S
- [Operator runbook](gaps/operability/runbook.md) — P1/M

#### Testing & QA ([`gaps/testing/`](gaps/testing/))
- [E2E on every PR](gaps/testing/e2e-on-pr.md) — **P0/S**
- [Lighthouse CI](gaps/testing/lighthouse-ci.md) — P2/S
- [Load tests (k6 / Gatling)](gaps/testing/load-tests.md) — P2/M
- [Chaos / fault injection](gaps/testing/chaos-fault-injection.md) — P2/S
- [Contract testing (Pact / OpenAPI diff)](gaps/testing/contract-testing.md) — P2/S
- [Visual regression testing](gaps/testing/visual-regression.md) — P2/S
- [Mutation testing target](gaps/testing/mutation-testing.md) — P2/S

#### CI/CD ([`gaps/cicd/`](gaps/cicd/))
- [Image push to registry](gaps/cicd/image-push-registry.md) — **P0/S**
- [Image signing (cosign)](gaps/cicd/cosign-image-signing.md) — P2/S
- [Renovate / Dependabot](gaps/cicd/renovate-dependabot.md) — **P0/S**
- [Pre-commit hooks (lint / format)](gaps/cicd/pre-commit-hooks.md) — P2/S
- [Release tagging + changelog](gaps/cicd/release-tagging.md) — P1/S

#### Compliance & data ([`gaps/compliance/`](gaps/compliance/))
- [Data retention policy](gaps/compliance/data-retention-policy.md) — P2/S
- [Account deletion + GDPR-style data export](gaps/compliance/account-deletion-gdpr.md) — P2/M
- [Cookie / storage disclosure](gaps/compliance/cookie-storage-disclosure.md) — P2/S

#### Documentation ([`gaps/documentation/`](gaps/documentation/))
- [Restore guide](gaps/documentation/restore-guide.md) — **P0/S**
- [On-call playbook](gaps/documentation/on-call-playbook.md) — P2/S
- [Threat model](gaps/documentation/threat-model.md) — P2/M
- [Architecture Decision Records (ADRs)](gaps/documentation/adrs.md) — P2/S
- [API reference (OpenAPI)](gaps/documentation/api-reference.md) — P1/S

---

## Quick wins (top 10 high-value, low-effort)

These are P0 or P1 items sized **S** that unlock outsized improvements:

1. [Security headers in nginx](gaps/security/security-headers.md) — minutes of edits, broad XSS/clickjacking defense
2. [Structured JSON logs](gaps/observability/structured-json-logs.md) — unblocks log aggregation and triage
3. [Dependency scanning (OWASP DC)](gaps/security/dependency-scanning.md) — surfaces existing CVEs
4. [Container scanning (Trivy)](gaps/security/container-scanning.md) — same, for OS packages
5. [Image push to GHCR](gaps/cicd/image-push-registry.md) — operators stop building locally
6. [Renovate / Dependabot](gaps/cicd/renovate-dependabot.md) — automatic patch PRs
7. [E2E on every PR](gaps/testing/e2e-on-pr.md) — catch integration regressions before merge
8. [Dark mode](gaps/settings/dark-mode.md) — universally requested, mostly Tailwind variants
9. [Markdown rendering](gaps/notes/markdown-rendering.md) — basic note formatting
10. [MinIO bucket mirror](gaps/backup-dr/minio-backups.md) — second copy of every uploaded file

---

## Roadmap

### Now (P0, ~next 4 weeks)

> **All P0 items closed.** Production-readiness baseline complete; the items below are kept for historical reference. Next phase: **P2**.

<details>
<summary>Closed P0 production-readiness baseline</summary>

- [TLS + reverse proxy](gaps/security/tls-reverse-proxy.md)
- [Security headers](gaps/security/security-headers.md)
- [Structured JSON logs](gaps/observability/structured-json-logs.md)
- [Metrics endpoint (Prometheus)](gaps/observability/metrics-prometheus.md)
- [Postgres + MinIO automated backups](gaps/backup-dr/postgres-backups.md), [`minio-backups.md`](gaps/backup-dr/minio-backups.md)
- [Restore guide](gaps/documentation/restore-guide.md)
- [Dependency + container scanning in CI](gaps/security/dependency-scanning.md), [`container-scanning.md`](gaps/security/container-scanning.md)
- [Image push to GHCR](gaps/cicd/image-push-registry.md)
- [Renovate / Dependabot](gaps/cicd/renovate-dependabot.md)
- [E2E on every PR](gaps/testing/e2e-on-pr.md)
- [Token refresh + logout revocation](gaps/auth/token-refresh.md), [`logout-revocation.md`](gaps/auth/logout-revocation.md)
- [Password reset email](gaps/auth/password-reset-email.md)
- [Audit log viewer](gaps/admin/audit-log-viewer.md)
- [Soft delete + trash](gaps/notes/soft-delete-trash.md)
- [Dark mode](gaps/settings/dark-mode.md)
- [Markdown rendering](gaps/notes/markdown-rendering.md)
- [In-app file preview](gaps/files/file-preview.md)

</details>

### Next (P1, 1–2 quarters)

Depth and polish. ✓-marked items are landed in the current phase (semantic search activation, OTel + Sentry scaffolds, offline-read for notes).

- ✓ [Semantic search](gaps/search/semantic-vector.md) — Ollama-backed embeddings + pgvector cosine ranking, opt-in via `EMBEDDING_ENABLED`. ✓ [Advanced filters](gaps/search/advanced-filters.md) — date range, file MIME, has-reminder + smart toggle exposed in SearchPage. [Search-within-folder](gaps/search/search-within-folder.md) backend is wired (folderId param) but UI surfacing pending.
- ✓ [Per-item ACLs](gaps/sharing/per-item-acls.md) — backend V010 + ItemAccessController, frontend `<VisibilityControl>` on note + file detail with private/household/custom and per-user role picker. [Shared shopping lists](gaps/sharing/shared-shopping-lists.md) still open. ✓ [Real-time updates](gaps/sync/realtime-updates.md) — `useLiveUpdates` hook subscribes to `/api/events` SSE via fetch+stream (auth header), invalidates TanStack queries on `ItemUpdated` / `ChecklistItemToggled`.
- ✓ [i18n framework](gaps/i18n/framework.md) — i18next + react-i18next + LanguageDetector + ICU plural support, en/es/de bundles. ✓ [Language selection](gaps/settings/language-selection.md) — picker in Settings → Appearance.
- [Reminder snooze](gaps/reminders/snooze.md), [.ics export](gaps/reminders/ics-export.md), [timezone handling](gaps/reminders/timezone-handling.md)
- [Image transforms](gaps/files/image-transforms.md), [chunked upload](gaps/files/chunked-resumable-upload.md), [virus scan](gaps/files/virus-scan.md), [share links](gaps/files/share-link-expiry.md)
- ✓ [Bulk move](gaps/folders/bulk-move.md) (POST /api/items/move + frontend itemMoveApi), ✓ [drag-and-drop](gaps/folders/drag-and-drop.md) — @dnd-kit pointer sensor on FolderPage; drop notes/files/subfolders onto subfolder cards or breadcrumb to move. ✓ [Pin/favorite](gaps/notes/pin-favorite.md) — toggle button on note detail header. ✓ [Export](gaps/notes/export-pdf-md.md) — Markdown / PDF download menu on note detail.
- ✓ [Offline read for notes](gaps/pwa/offline-read-notes.md) — IndexedDB cache layer + cache-first hook on note detail. ✓ [Background sync edits](gaps/pwa/background-sync-edits.md) — IndexedDB edit queue + auto-flush on `online` event with optimistic-lock conflict handling.
- [Accessibility pass](gaps/a11y/keyboard-nav.md), [`aria-labels-landmarks.md`](gaps/a11y/aria-labels-landmarks.md), [`axe-ci.md`](gaps/a11y/axe-ci.md)
- ✓ [OpenTelemetry tracing](gaps/observability/opentelemetry-tracing.md) — Micrometer OTel bridge + OTLP exporter, sampling default 0.0. ✓ [Sentry](gaps/observability/frontend-error-tracking.md) — opt-in dynamic import + web-vitals. ✓ [Grafana dashboards](gaps/observability/grafana-dashboards.md) + ✓ [Loki](gaps/observability/log-aggregation.md) — opt-in via `docker-compose.observability.yml` (Prometheus + Grafana + Loki/Promtail + Tempo with provisioned datasources & dashboards).
- [Circuit breaker](gaps/reliability/circuit-breaker.md), [retry/backoff](gaps/reliability/retry-backoff.md), [graceful shutdown](gaps/reliability/graceful-shutdown.md), [idempotency keys](gaps/reliability/idempotency-keys.md)
- ✓ [Caffeine cache](gaps/performance/caffeine-cache.md), ✓ [slow-query logging](gaps/performance/slow-query-logging.md) — Hibernate `LOG_QUERIES_SLOWER_THAN_MS` env-tunable via `SQL_SLOW_THRESHOLD_MS` (default 500).
- [Operator runbook](gaps/operability/runbook.md), [release tagging](gaps/cicd/release-tagging.md), [API reference](gaps/documentation/api-reference.md)
- ✓ [Per-endpoint rate limits](gaps/security/per-endpoint-rate-limits.md) — login, register, password-reset, writes, comments, share-resolve, file-uploads. [Secret management](gaps/security/secret-management.md) still open. ✓ [Encryption at rest](gaps/security/encryption-at-rest.md) — ADR-0006 documents disk-level (LUKS) as the v1 posture, defers `pgcrypto`/SSE/client-side until a household need + KMS choice.
- [Invite-only registration](gaps/admin/invite-flow.md)

### Later (P2)

Strategic / nice-to-have:

- [MFA (TOTP)](gaps/auth/mfa-totp.md), [OIDC](gaps/auth/oidc-sso.md)
- ✓ [Comments + @mentions](gaps/sharing/comments-mentions.md) — comment thread + checkbox picker shipped earlier; this round added inline `@`-autocomplete dropdown over the user roster + groups. [Real-time co-edit (CRDT)](gaps/sharing/co-edit.md) still open.
- ✓ [Note version history](gaps/notes/version-history.md) — `<RevisionsPanel>` lists captured edits with restore (auto-snapshots current head). ✓ [Templates](gaps/notes/templates.md) — picker chips on the New Note page + "Save as template" toggle in the editor.
- [File versioning](gaps/files/versioning.md)
- [Household / group concept](gaps/admin/household-group-concept.md), ✓ [usage analytics](gaps/admin/usage-analytics.md) — `/admin/usage` page renders totals, top storage users, top tags, top folders. [Bulk user import](gaps/admin/bulk-user-import.md) still open.
- [Child-safe content moderation](gaps/child-safe/moderation-hooks.md), [parental review queue](gaps/child-safe/parental-review-queue.md), ✓ [time limits / quiet hours](gaps/child-safe/time-limits.md) — `users.quiet_hours_start/_end` enforced in `PushService`, settings UI added.
- [GDPR data export](gaps/settings/gdpr-export.md), [self-service account deletion](gaps/settings/account-deletion.md)
- [Threat model](gaps/documentation/threat-model.md), [ADRs](gaps/documentation/adrs.md), [on-call playbook](gaps/documentation/on-call-playbook.md)
- [SBOM](gaps/security/sbom.md), [cosign signing](gaps/cicd/cosign-image-signing.md)
- [Load testing](gaps/testing/load-tests.md), [chaos](gaps/testing/chaos-fault-injection.md), [visual regression](gaps/testing/visual-regression.md), [contract testing](gaps/testing/contract-testing.md)

---

## Maintenance

This document and the per-gap files are intended to be **living**. As gaps are closed, mark the file with a top-banner `> **Status:** Closed in <PR/release>` rather than deleting it — the references and rationale remain useful history. New gaps follow the same one-file-per-entry convention.
