# Authentication and User Management

## 1. Registration

### `POST /api/auth/register`

**Auth required:** No

**Request body:**
```json
{
  "email": "string",
  "password": "string",
  "displayName": "string"
}
```

**Validation rules:**
- `email` — RFC 5322 format, validated with Jakarta `@Email`; max 320 chars
- `password` — minimum 8 characters; at least 1 uppercase letter, 1 lowercase letter, 1 digit
- `displayName` — 1–100 characters; no control characters (regex: `^[^\p{Cntrl}]+$`)

**Business rules:**
- The first account ever registered in the system automatically receives `is_admin = true`
- All subsequent registrations: `is_admin = false`, `is_child = false`
- Duplicate email: `409 Conflict` with `{"code": "EMAIL_ALREADY_EXISTS"}`
- On success: `201 Created` with the same response shape as login (includes JWT)

**Response (201):** Same as login response — see Section 2.

---

## 2. Login

### `POST /api/auth/login`

**Auth required:** No

**Request body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Response (200):**
```json
{
  "token": "string",
  "expiresAt": "2025-04-26T10:30:00Z",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "displayName": "Jane",
    "isAdmin": false,
    "isChild": false
  }
}
```

**Error cases:**
- Wrong password or unknown email: `401` with `{"code": "INVALID_CREDENTIALS"}` — do NOT distinguish between the two
- Account disabled (`is_active = false`): `403` with `{"code": "ACCOUNT_DISABLED"}`

---

## 3. JWT Configuration

| Property | Value |
|----------|-------|
| Algorithm | HS256 |
| Signing key | `JWT_SECRET` env var (base64-encoded, minimum 32 characters) |
| Token lifetime | `JWT_EXPIRY_HOURS` env var (default `24`) |
| Token refresh | Not supported in v1 — client must re-login after expiry |
| Blacklist | Not supported in v1 |

**JWT Claims:**
```json
{
  "sub": "1",
  "email": "user@example.com",
  "isAdmin": false,
  "isChild": false,
  "iat": 1714041600,
  "exp": 1714128000
}
```

**Filter behaviour:**
- Every protected request passes through `JwtAuthenticationFilter` (extends `OncePerRequestFilter`)
- Filter validates signature, expiry, and that `sub` maps to an active user
- On valid token: sets `SecurityContextHolder` with a `UserPrincipal` containing id, email, isAdmin, isChild
- On missing token: returns `401 UNAUTHORIZED`
- On expired token: returns `401 TOKEN_EXPIRED`
- On invalid signature or malformed token: returns `401 UNAUTHORIZED`
- The filter consults the database only to verify `is_active = true`; all other claims are trusted from the token

---

## 4. Current User

### `GET /api/auth/me`

**Auth required:** Yes

**Response (200):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "displayName": "Jane",
  "isAdmin": false,
  "isChild": false
}
```

---

### `PUT /api/auth/me`

**Auth required:** Yes

**Request body:**
```json
{
  "displayName": "string",
  "currentPassword": "string",
  "newPassword": "string"
}
```

**Rules:**
- `displayName` — optional; 1–100 chars if provided
- `currentPassword` + `newPassword` — both required if either is provided; `currentPassword` must match the stored hash; `newPassword` must meet password complexity rules
- If only updating `displayName`, omit both password fields
- Returns `400 WRONG_CURRENT_PASSWORD` if `currentPassword` does not match

**Response (200):** Updated user object (same shape as `GET /api/auth/me`).

---

## 5. Admin: User Management

All endpoints in this section require `is_admin = true`. Enforced via Spring Security `@PreAuthorize("hasRole('ADMIN')")` or equivalent.

Non-admin access returns `403 FORBIDDEN`.

---

### `GET /api/admin/users`

List all users. Returns an array (not paginated — household scale is small).

**Response (200):**
```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "displayName": "Jane",
    "isAdmin": true,
    "isChild": false,
    "isActive": true,
    "createdAt": "2025-04-25T10:30:00Z"
  }
]
```

`password_hash` is never returned in any response.

---

### `POST /api/admin/users`

Create a new user account.

**Request body:**
```json
{
  "email": "string",
  "password": "string",
  "displayName": "string",
  "isAdmin": false,
  "isChild": false
}
```

**Rules:**
- Same validation as registration
- `isAdmin` and `isChild` cannot both be `true` — `400 VALIDATION_ERROR` with `{"field": "isChild", "message": "Account cannot be both admin and child"}`
- Duplicate email: `409 EMAIL_ALREADY_EXISTS`

**Response (201):** Created user object.

---

### `PUT /api/admin/users/{id}`

Update an existing user.

**Request body:**
```json
{
  "displayName": "string",
  "isAdmin": false,
  "isChild": false,
  "isActive": true
}
```

**Rules:**
- Cannot set `isAdmin = false` on own account (prevent admin lockout): `400 CANNOT_REMOVE_OWN_ADMIN`
- Cannot set `isAdmin = true` and `isChild = true` simultaneously: `400 VALIDATION_ERROR`
- `isActive` — set to `false` to disable the account (soft delete)

**Response (200):** Updated user object.

---

### `DELETE /api/admin/users/{id}`

Soft-deletes a user by setting `is_active = false`.

**Rules:**
- Cannot delete own account: `400 CANNOT_DELETE_SELF`

**Response (204):** No content.

---

### `POST /api/admin/users/{id}/reset-password`

Reset another user's password (e.g. for a child who forgot theirs).

**Request body:**
```json
{
  "newPassword": "string"
}
```

**Rules:**
- `newPassword` must meet password complexity rules
- Admin cannot use this to reset their own password — use `PUT /api/auth/me` instead

**Response (204):** No content.

---

## 6. Security Constraints

| Constraint | Value |
|-----------|-------|
| Password hashing | bcrypt |
| bcrypt cost factor | `BCRYPT_COST` env var, default `12`, valid range 10–14 |
| Token storage | Client-side (localStorage or memory); backend is stateless |
| Rate limiting | Out of scope for v1; document as future concern |

- `password_hash` must never appear in any API response, log line, or exception message
- Login timing must be consistent (bcrypt comparison runs even for unknown emails to prevent timing attacks — use a dummy hash comparison)

## 7. CSRF posture

Spring Security has CSRF protection **disabled** in `SecurityConfig`. This is correct for the current architecture, and the rationale is recorded here so a future change does not silently invalidate the assumption.

**Why CSRF is not a threat today:**

- Auth tokens travel only in the `Authorization: Bearer <jwt>` header, never in cookies. CSRF attacks rely on the browser auto-attaching authentication credentials (cookies) to a forged cross-site request — without an ambient credential, the forged request is unauthenticated and is rejected by `JwtAuthFilter`.
- `Authorization` cannot be set on a cross-origin request without preflight; `Origin` is checked by `CorsConfig` against `CORS_ALLOWED_ORIGINS`.
- The frontend stores tokens in `localStorage`, which is partitioned per-origin and unreachable to other sites' JavaScript.

**When this changes — re-evaluate:**

- If refresh tokens move from request body to `HttpOnly` cookie (a common hardening step), the refresh endpoint becomes CSRF-vulnerable. Mitigations: set `SameSite=Strict; Secure` on the cookie and require a header-confirmed double-submit token, or scope the cookie with `Path=/api/auth/refresh` so it only attaches to the one endpoint that needs it.
- If session-based auth replaces JWT, re-enable Spring's CSRF filter and provide the token via a meta tag the frontend reads.
- If the frontend ever embeds another origin's content in an iframe, the existing `Content-Security-Policy: frame-ancestors 'none'` header (set by `frontend/nginx.conf.template`) is the relevant control, not CSRF — but the `X-Frame-Options: DENY` header is the belt-and-braces default.
