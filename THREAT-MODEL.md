# Threat model

A short, opinionated security map for Home KM. The point is to make defended-vs-accepted threats explicit so future contributors don't accidentally regress a decision (or accidentally over-engineer one). STRIDE-style.

Revisit yearly or when a new asset/trust-boundary lands.

## Assets

| Asset | Where it lives | Why it matters |
|-------|----------------|----------------|
| Notes (titles, bodies, checklist items) | Postgres `notes`, `checklist_items` | Often contain household-personal content (medical, finance). |
| Files | MinIO blob + `files` row | PDFs, photos, receipts. Some sensitive (IDs, statements). |
| Credentials (bcrypt hashes) | Postgres `users.password_hash` | Disclosure → offline crack → account takeover. |
| Access JWTs + refresh tokens | Frontend localStorage; refresh hashes in DB | Bearer tokens — possession ≈ identity. |
| `JWT_SECRET` | Operator's `.env` | Signs every JWT. Rotation procedure: `docs/jwt-rotation.md`. |
| MinIO root credentials | `.env`, `MINIO_ROOT_USER/_PASSWORD` | Full read/write on every blob. |
| VAPID private key | `.env` | Signs push messages; disclosure lets an attacker push notifications as the household. |
| Audit log | Postgres `audit_events` | Forensic trail. Tampering hides incidents. |

## Trust boundaries

```
[ Browser / PWA ] --HTTPS--> [ nginx (frontend) ] --HTTP, infra-net--> [ Spring Boot API ]
                                                                     |--> [ Postgres ]
                                                                     |--> [ MinIO ]
                                                                     '--> [ Push gateway ]
```

- **Browser ↔ frontend nginx:** TLS terminates at the operator's reverse proxy (Caddy / Cloudflare / Traefik — see `docs/tls.md`). Inside the host, nginx talks plaintext to the API on the docker bridge.
- **API ↔ Postgres:** plaintext on the bridge, credentials in env vars.
- **API ↔ MinIO:** plaintext on the bridge; presigned URLs are signed with HMAC-SHA-256 against the MinIO secret and expire in 15 min by default.
- **API ↔ push gateway:** outbound HTTPS, VAPID-signed.

The household LAN is treated as **semi-trusted**. Anyone with LAN access can reach the unencrypted nginx → API hop unless TLS terminates on a public-facing reverse proxy. The CSP + security-headers (`frontend/nginx.conf.template`) push back on what a malicious LAN device can do via a phished browser.

## STRIDE

### Spoofing

| Threat | Mitigation | Status |
|--------|------------|--------|
| Forge a JWT to impersonate a user. | HS256 with `JWT_SECRET ≥ 32 chars`; verified on every request via `JwtAuthFilter`. | **Mitigated** |
| Replay a stolen JWT. | Access tokens TTL ≤ 24h; refresh tokens are stored hashed and revocable. Logout revokes via `JwtDenylist` + refresh-token row. | **Mitigated** (windowed) |
| Brute-force login. | `LoginRateLimiter` 20 attempts / IP / 60s; bcrypt cost 12. | **Mitigated** |
| Phish credentials via lookalike origin. | Bearer token (no SameSite cookies), CSP `frame-ancestors 'none'`, X-Frame-Options DENY. | **Mitigated** for clickjacking. Visual phishing is **accepted** at this scale. |
| Forge a push notification. | VAPID-signed; private key stays on the API server. | **Mitigated** |

### Tampering

| Threat | Mitigation | Status |
|--------|------------|--------|
| Modify a row in transit between API and DB. | Plaintext over the docker bridge — relies on host integrity. | **Accepted** (single-host trust). |
| Modify an audit row. | DB role principle: app user can `INSERT INTO audit_events`, `SELECT` for the viewer; no `UPDATE`/`DELETE` from the app layer. | **Mitigated** |
| Tamper with a release artefact in transit. | Cosign keyless OIDC signs every release image; verification recipe in RUNBOOK. CycloneDX SBOMs attest to image contents. | **Mitigated** if operator verifies before pull. |
| Tamper with a presigned URL to read someone else's blob. | URLs include `${userId}/...` prefix and HMAC over the path. | **Mitigated** |

### Repudiation

| Threat | Mitigation | Status |
|--------|------------|--------|
| User denies an action they took. | `AuditService.record` logs every state-changing endpoint with actor + entity + timestamp. Surfaced in the admin audit-log viewer. | **Mitigated** |

### Information disclosure

| Threat | Mitigation | Status |
|--------|------------|--------|
| XSS exfiltrates the JWT from localStorage. | rehype-sanitize on every markdown render; CSP `script-src 'self'`; no third-party JS, no inline scripts. ADR-0002 records the localStorage decision. | **Mitigated** |
| Eavesdrop on browser ↔ nginx. | Operator-supplied TLS proxy (`docs/tls.md`). Without TLS, plaintext over LAN. | **Operator responsibility**, documented. |
| `password_hash` leaks via API/log. | `User` entity excludes from JSON; `MdcFilter` redacts; spec `02-auth.md` § 6 mandates. | **Mitigated** |
| Slow / Postgres logs leak query parameters. | `log_min_duration_statement=500ms`; `pg_stat_statements` does NOT capture parameters by default. | **Mitigated** |
| Backup blob containing notes lands on an untrusted storage target. | Restic `--encrypt` with operator-supplied passphrase (`docs/backups.md`); MinIO mirror points to a household-controlled second instance. | **Mitigated** if operator follows docs. |
| Search returns content child user shouldn't see. | Every search branch filters `is_child_safe = true` for child principals (see `SearchService.search`). | **Mitigated** |
| Image EXIF / metadata leaks location. | **Accepted** at this scale — household members upload their own files knowingly. Re-evaluate if file-sharing-with-strangers ever lands. |

### Denial of service

| Threat | Mitigation | Status |
|--------|------------|--------|
| Login flood drains bcrypt CPU. | `LoginRateLimiter` 20/IP/60s. | **Mitigated** |
| Per-endpoint flood saturates Tomcat. | `RateLimitFilter` with per-rule env-configurable thresholds. | **Mitigated** |
| Huge file upload starves disk. | `MAX_FILE_UPLOAD_MB` (default 100MB); nginx `client_max_body_size`. | **Mitigated** |
| Unbounded paginated query (`?size=99999`). | `Pagination.clampSize` (max 100) on every list endpoint. | **Mitigated** |
| MinIO outage takes down API thread pool. | Resilience4j circuit breaker (`minio` instance). ADR-0005 + `ChaosToxiproxyTest`. | **Mitigated** |
| Recursive folder hierarchy explodes search. | `MAX_FOLDER_DEPTH=20` enforced on create + move. | **Mitigated** |

### Elevation of privilege

| Threat | Mitigation | Status |
|--------|------------|--------|
| Child user mutates content. | `ChildAccountWriteException` thrown by every write path; `is_child` checked early in service methods. | **Mitigated** |
| Non-admin invokes admin endpoint. | `@PreAuthorize("hasRole('ADMIN')")` on `/api/admin/**`. | **Mitigated** |
| Last admin self-deactivates. | `AuthService.deactivateSelf` rejects when `countOtherActiveAdmins == 0` (returns 409 LAST_ADMIN). | **Mitigated** |
| SQL injection via search query. | Native queries use `:named` parameters; never string concat user input. CodeQL / OWASP DC scans transitively. | **Mitigated** |
| Path traversal via filename. | `sanitizeFilename` strips `/`, `\`, `..`. | **Mitigated** |

## Out of scope (explicitly accepted)

- **State-level adversary** with physical access to the host. Household app, single-host. If they own the box, they own the data.
- **Side-channel attacks** (Spectre, Rowhammer). Same reason.
- **Compromised dependency at install time.** Mitigated *partially* by Dependabot + OWASP DC + cosign on our images, but a backdoored upstream npm package still ships through. Detection at runtime is out of scope.
- **Multi-tenant isolation.** ADR-0001 records the single-tenant scope — re-evaluate if the household-group concept ever lands (`gaps/admin/household-group-concept.md`).

## Re-evaluate when…

- Auth posture changes (cookie sessions, OIDC, MFA) → re-do Spoofing.
- Per-item ACLs land → re-do Information disclosure.
- The deployment topology grows beyond one host → re-do Tampering / Information disclosure.
- A new external dependency (LLM, AV scanner, email gateway) joins the trust boundary diagram → re-do all five categories for that edge.

## See also

- `specs/02-auth.md` § 7 (CSRF posture)
- `docs/jwt-rotation.md`
- `docs/encryption.md`
- `docs/RUNBOOK.md` § "Verifying signed images" (cosign)
- `PRIVACY.md`, `RETENTION.md`
