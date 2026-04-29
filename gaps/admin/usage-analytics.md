# Usage analytics dashboard

| Field | Value |
|---|---|
| Category | Functional · Admin |
| Priority | P2 |
| Size | S |
| Status | Closed |

**Current state:** No insight into how the household actually uses the app. Storage growth, active-user counts, file-type breakdown — all invisible without manual SQL.

**Gap:** No usage dashboard for the admin.

**Proposed direction:** A read-only `/admin/usage` page driven by aggregate queries: total items by type, storage used per user (sum of `stored_files.size_bytes` grouped), active users in last 30 days (from `users.last_login_at` once added), top tags, top folders by item count, reminder volume. Pure reporting — no PII beyond per-user totals. Optionally surface metrics via the same Prometheus endpoint added in `non-functional/observability/metrics-prometheus.md`.

**References:** `backend/src/main/java/com/homekm/admin/AdminController.java`, `backend/src/main/java/com/homekm/auth/User.java`, `backend/src/main/java/com/homekm/file/StoredFile.java`, `frontend/src/features/admin/`
