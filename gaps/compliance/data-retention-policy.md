# Data retention policy

| Field | Value |
|---|---|
| Category | Non-functional · Compliance & data |
| Priority | P2 |
| Size | S |

**Current state:** Data lives forever unless explicitly deleted. No documented retention policy for any data class — notes, files, push subscriptions, audit logs, search history.

**Gap:** Operators have no guidance on what to keep, for how long, or why.

**Proposed direction:** Document defaults: notes/files retained until deleted; soft-deleted items purged after 30 days (`notes/soft-delete-trash.md`); audit logs 1 year (`admin/audit-log-viewer.md`); push subscriptions removed after 410-Gone or 6 months idle. All retention windows configurable. Document in `RETENTION.md` and reference from spec `00-overview.md`.

**References:** `backend/src/main/java/com/homekm/push/PushService.java`, `backend/src/main/java/com/homekm/admin/`, `specs/00-overview.md`
