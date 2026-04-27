# Audit log viewer

| Field | Value |
|---|---|
| Category | Functional · Admin |
| Priority | P0 |
| Size | M |
| Status | Closed |

**Current state:** No audit log. Admin actions (user creation, password reset, role change, bulk delete) leave no trace beyond stdout logs in the container.

**Gap:** Cannot answer "who deleted my note?" or "when was Dad given admin?". Compliance and trust both suffer.

**Proposed direction:** `audit_events` table (id, actor_user_id, action, target_type, target_id, before JSONB, after JSONB, ip, user_agent, occurred_at). Capture via a Spring AOP advice on `@Audited` methods or in service classes for high-value flows: auth, admin, delete, ACL change, share-link create, password reset. Admin page lists with filters by actor, action, date range. Retain 1 year by default.

**References:** `backend/src/main/java/com/homekm/admin/AdminService.java`, `backend/src/main/java/com/homekm/auth/AuthController.java`, `backend/src/main/java/com/homekm/common/`, `specs/02-auth.md`
