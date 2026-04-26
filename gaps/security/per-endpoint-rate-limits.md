# Per-endpoint rate limits

| Field | Value |
|---|---|
| Category | Non-functional · Security |
| Priority | P1 |
| Size | S |

**Current state:** Only `LoginRateLimiter` (20 attempts / 60s / IP) exists. All other endpoints (`POST /api/notes`, `POST /api/files`, push subscribe, password change) are unbounded.

**Gap:** A compromised account or buggy client can write hundreds of notes per second; a public-facing instance is open to enumeration on `/api/auth/register`.

**Proposed direction:** Generalize `LoginRateLimiter` into a `RateLimitFilter` configurable per-endpoint (e.g., `app.rate-limits.endpoints[POST /api/files]=10/min/user`). Use Bucket4j for in-memory limits; persist counters in Redis later if multi-instance becomes a concern. Defaults: writes 60/min/user, register 5/hour/IP, password-reset request 3/hour/IP.

**References:** `backend/src/main/java/com/homekm/auth/LoginRateLimiter.java`, `backend/src/main/java/com/homekm/common/`, `backend/src/main/java/com/homekm/auth/AuthController.java`, `specs/02-auth.md`
