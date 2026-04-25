# API Conventions

All backend API endpoints share the conventions defined in this document. Feature specs reference this document rather than repeating these rules.

---

## 1. Base Path

All API endpoints are prefixed with `/api`. Each Spring Boot controller class uses `@RequestMapping("/api/...")` explicitly — the Spring Boot server context path is not modified.

Example: `@RequestMapping("/api/notes")` on `NoteController` → full path `/api/notes`.

---

## 2. Authentication Header

Protected endpoints require:

```
Authorization: Bearer <JWT>
```

Missing or malformed token → `401 Unauthorized`:
```json
{ "code": "UNAUTHORIZED", "message": "Authentication required", "timestamp": "2025-04-25T10:30:00Z" }
```

Expired token → `401 Unauthorized`:
```json
{ "code": "TOKEN_EXPIRED", "message": "Token has expired", "timestamp": "2025-04-25T10:30:00Z" }
```

The JWT filter must distinguish these two cases. Public endpoints (no auth required): `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/vapid-public-key`.

---

## 3. Error Envelope

All error responses use a consistent JSON envelope.

**Single error:**
```json
{
  "code": "SNAKE_CASE_ERROR_CODE",
  "message": "Human-readable description for debugging",
  "timestamp": "2025-04-25T10:30:00Z"
}
```

**Validation error (multiple fields):**
```json
{
  "code": "VALIDATION_ERROR",
  "errors": [
    { "field": "email", "message": "must be a valid email address" },
    { "field": "password", "message": "must be at least 8 characters" }
  ],
  "timestamp": "2025-04-25T10:30:00Z"
}
```

**Spring `@ControllerAdvice` must handle:**

| Exception | HTTP Status | Code |
|-----------|-------------|------|
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `ConstraintViolationException` | 400 | `VALIDATION_ERROR` |
| `EntityNotFoundException` (custom) | 404 | `NOT_FOUND` |
| `AccessDeniedException` | 403 | `FORBIDDEN` |
| `ChildAccountWriteException` (custom) | 403 | `CHILD_ACCOUNT_READ_ONLY` |
| Any unhandled `Exception` | 500 | `INTERNAL_ERROR` |

Never include a stack trace in the response body. Log it server-side at ERROR level.

---

## 4. Pagination Envelope

All list endpoints that support pagination return this envelope:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

Query parameters for paginated endpoints:
- `page` — zero-based page index, default `0`
- `size` — page size, default `20`, max `100`

Implement using Spring Data `Page<T>` mapped to a `PageResponse<T>` DTO.

---

## 5. HTTP Methods and Status Codes

| Operation | Method | Success Status | Response Body |
|-----------|--------|---------------|---------------|
| Fetch resource | GET | 200 OK | Resource object or page envelope |
| Create resource | POST | 201 Created | Created resource object |
| Full update | PUT | 200 OK | Updated resource object |
| Delete resource | DELETE | 204 No Content | Empty |
| Validation failure | — | 400 Bad Request | Error envelope |
| Auth required | — | 401 Unauthorized | Error envelope |
| Forbidden | — | 403 Forbidden | Error envelope |
| Not found | — | 404 Not Found | Error envelope |
| Conflict | — | 409 Conflict | Error envelope |
| Server error | — | 500 Internal Server Error | Error envelope |

---

## 6. CORS Configuration

Spring Security CORS config reads from `CORS_ALLOWED_ORIGINS` env var (comma-separated URLs, default `http://localhost:3000`).

```
Allow-Origins: values from CORS_ALLOWED_ORIGINS
Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Allow-Headers: Authorization, Content-Type, X-Requested-With
Allow-Credentials: false
Max-Age: 3600
```

`allowCredentials: false` — JWT is in the `Authorization` header, not a cookie.

---

## 7. Content-Type

All API request bodies and responses use `application/json` except:
- File uploads: `multipart/form-data`
- File binary replacement: `multipart/form-data`

All controllers set `produces = MediaType.APPLICATION_JSON_VALUE`. File upload endpoints additionally accept `MediaType.MULTIPART_FORM_DATA_VALUE`.

---

## 8. Timestamps

All datetime fields are returned as ISO-8601 strings in UTC:

```
"2025-04-25T10:30:00Z"
```

Backend stores all datetimes as `TIMESTAMPTZ` in PostgreSQL. Jackson configuration:
```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
      write-dates-with-context-time-zone: true
    time-zone: UTC
```

Incoming datetime strings from clients must include a timezone offset (`Z` or `+HH:MM`). Strings without timezone are rejected with `400 VALIDATION_ERROR`.

---

## 9. ID Type

All entity IDs are `BIGINT` in PostgreSQL and serialized as JSON **numbers** (not strings). Clients must handle 64-bit integers. Example: `"id": 1234567890`.

---

## 10. Child Account Write Enforcement

Any request that performs a write operation (POST, PUT, DELETE) from a child account — except for creating/editing the child's own notes and files — must be rejected at the **service layer** (not controller layer) with `403` and code `CHILD_ACCOUNT_READ_ONLY`.

Operations allowed for child accounts:
- `POST /api/notes` — create own note (auto child-safe)
- `PUT /api/notes/{id}` — edit own note only
- `POST /api/files/upload` — upload own file (auto child-safe)
- `PUT /api/files/{id}` — edit metadata of own file only
- `POST /api/push/subscribe` / `DELETE /api/push/subscribe` — manage own push subscriptions
- `PUT /api/auth/me` — update own display name / password

Operations **blocked** for child accounts (return `403 CHILD_ACCOUNT_READ_ONLY`):
- DELETE on any resource
- PUT on any resource not owned by the child
- POST on folders
- PUT/DELETE on folders
- Any admin endpoint
- Changing `is_child_safe` flag on any item
