# Home KM — Operations Runbook

## Quick reference

| Task | Command |
|------|---------|
| Start everything | `docker compose -f docker-compose.infra.yml up -d && docker compose -f docker-compose.app.yml up -d` |
| Stop everything | `docker compose -f docker-compose.app.yml down && docker compose -f docker-compose.infra.yml down` |
| Tail API logs | `docker compose -f docker-compose.app.yml logs -f api` |
| Tail DB logs | `docker compose -f docker-compose.infra.yml logs -f postgres` |
| Restart API after image rebuild | `docker compose -f docker-compose.app.yml up -d --build api` |
| API readiness | `curl -fsS localhost:8080/actuator/health/readiness` |
| API metrics | `curl -fsS localhost:8080/actuator/prometheus` (admin token required for non-anonymous) |

## Backup & restore

### Postgres
```bash
docker compose -f docker-compose.infra.yml exec -T postgres \
  pg_dump -U "$DB_USER" "$DB_NAME" > backups/homekm-$(date +%F).sql
```
Restore (drops and reloads):
```bash
docker compose -f docker-compose.infra.yml exec -T postgres \
  psql -U "$DB_USER" "$DB_NAME" < backups/homekm-2026-04-27.sql
```

### MinIO
```bash
mc alias set homekm http://localhost:9000 "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY"
mc mirror --overwrite homekm/homekm backups/minio/
```

See `docs/restore-drill.md` for the monthly verification procedure and `docs/backups.md` for off-host restic configuration.

## Rotating the JWT secret

See `docs/jwt-rotation.md`. Summary: change `JWT_SECRET` (32+ chars), restart API. All access tokens become invalid; users get a 401 and the frontend will refresh from the refresh token (which is stored hashed and not invalidated by JWT_SECRET change). To force everyone out, also revoke all refresh tokens:
```sql
UPDATE refresh_tokens SET revoked_at = now() WHERE revoked_at IS NULL;
```

## Rotating the TLS certificate

If you front the app with a reverse proxy (Caddy/Traefik), restart the proxy after dropping in the new certificate. Browsers will pick it up on the next request — no app changes required.

## Releasing a new version

1. Merge to `master` with [Conventional Commit](https://www.conventionalcommits.org/) messages.
2. The `release-please` workflow (`.github/workflows/release.yml`) opens/updates a release PR with a generated CHANGELOG.
3. Review and merge that PR. It tags `vX.Y.Z`, which the image-build workflow reads to tag Docker images.
4. Pull the new image and `docker compose -f docker-compose.app.yml up -d --build`.

Graceful shutdown is wired (`server.shutdown=graceful`, 30s phase timeout, 35s `stop_grace_period`); existing in-flight requests will finish.

## On-call playbook

For symptom → fix entries (5xx, upload failures, push, login, disk full, etc.), see `PLAYBOOK.md` at the repo root. This runbook covers routine operations; the playbook covers incidents.

## Common errors

| Symptom | Cause | Fix |
|---------|-------|-----|
| Login returns 403 (CORS) | Browser origin not in `CORS_ALLOWED_ORIGINS` | Add the LAN IP / hostname and restart API |
| 503 with `STORAGE_UNAVAILABLE` | MinIO circuit breaker open | Check MinIO container: `docker compose -f docker-compose.infra.yml logs minio`; the breaker auto-resets after 30s of healthy calls |
| 429 with `RATE_LIMITED` | A client exceeded `app.rate-limit` rules | Check `RateLimitFilter` rule that fired in API logs; increase per-rule limits in env or back off the client |
| Slow queries | Set `log_min_duration_statement=500ms` is on; review postgres logs | Use `EXPLAIN ANALYZE` for the SQL and add indexes; consider `pg_stat_statements` |
| Reminders not firing | Scheduler skipped during shutdown | Confirm `ShutdownState.isShuttingDown()` is false; restart API |

## Slow query analysis

PostgreSQL is configured (via `infra/postgres/init.sql`) to log statements over 500ms:
```bash
docker compose -f docker-compose.infra.yml logs postgres | grep "duration:"
```
Pull a representative slow query into `psql`, prefix with `EXPLAIN (ANALYZE, BUFFERS)`. Common culprits: missing indexes on `folder_id`, full-text query without `tsvector`, child-safe filter on a non-indexed column.

## Observability stack

The default deploy ships only `/actuator/prometheus`. To run a full Grafana/Prometheus/Loki/Tempo stack, bring up `docker-compose.observability.yml` (see `docs/observability.md`).

## Service Level Objectives

See `docs/slo.md` for the full table. TL;DR for paging decisions:

- API availability < 99.5% over 10 min → page on-call (warning).
- Read p95 > 500 ms over 10 min → investigate (warning).
- Push delivery < 95% over 1 h → check FCM/APNS bridge (warning).

Recovery targets: **RTO 4 h, RPO 1 h**. If a restore would take longer or lose more data than that, the backup cadence is broken — fix it before declaring the restore drill green.

## Container log retention

Each compose service caps its `json-file` log driver at a few hundred MB
(`max-size` × `max-file`). Without this, Docker's default unlimited log
file fills disks silently. Tunings:

| Service | max-size × max-file | Rationale |
|---------|--------------------|-----------|
| `api` | 50m × 5 | Chatty during request bursts; structured JSON is verbose. |
| `postgres` | 50m × 5 | Slow-query log + extension warnings. |
| `minio` | 50m × 5 | Mostly access logs; the bucket-event firehose is verbose. |
| `frontend` (nginx) | 20m × 3 | Just access + error logs. |

When `docker-compose.observability.yml` is up, Loki ingests these files
on a 30-day retention; the per-driver caps still apply as a floor.

## Build / version info

- `/api/info` returns `{ build: { name, version, time }, git: { branch, commitId, commitTime } }` populated by the gradle `springBoot.buildInfo()` task and the gradle-git-properties plugin.
- The frontend Settings → About panel surfaces the same payload — ask users to copy that block into bug reports.
- `/actuator/info` exposes the full Spring Boot info contributor set (env disabled to avoid leaking secret env-var names).

## Break-glass admin reset

If the sole admin has lost their credentials and no other admin is available, run `scripts/admin-reset.sh <email>` from the host. The script bcrypts a new password, writes it directly into Postgres, and revokes every refresh token for that account. Source your `.env` first so `DB_USER` and `DB_NAME` are exported.

This is intended for genuine lockouts. Routine resets go through `/api/admin/users/{id}/reset-password` or the user's own `/api/auth/password-reset/request` flow.

## Reproducible builds

Three layers keep the same commit producing the same artefact months apart:

1. **Backend** — Gradle dependency locking is enabled (`dependencyLocking { lockAllConfigurations(); lockMode = STRICT }`). `gradle.lockfile` is committed; CI will fail if the resolved versions drift. To intentionally update a dep, run `./gradlew dependencies --write-locks` and commit the new lockfile alongside the build change.
2. **Frontend** — `frontend/package-lock.json` is committed; CI uses `npm ci` (refuses to install anything not in the lockfile). The Alpine-musl Dockerfile uses `npm install` instead — see CLAUDE.md for the platform-deps rationale.
3. **Docker** — base images are pinned to major tags (`node:24-alpine`, `eclipse-temurin:21-jre-alpine`). Dependabot monitors them weekly via `.github/dependabot.yml` and opens digest-updating PRs. We do not commit `@sha256:` digests directly because every base-image security rebuild would otherwise require a manual ratchet.

## Verifying signed images

Release images are signed with cosign keyless OIDC (Sigstore). Verify before pulling on a sensitive host:

```bash
cosign verify ghcr.io/wliant/home-km/api:vX.Y.Z \
  --certificate-identity-regexp '^https://github.com/wliant/home-km/' \
  --certificate-oidc-issuer 'https://token.actions.githubusercontent.com'
```

Each release also publishes CycloneDX SBOMs as release assets and as cosign attestations on the image. Inspect the attestation:

```bash
cosign download attestation ghcr.io/wliant/home-km/api:vX.Y.Z | jq -r .payload | base64 -d | jq .
```

## Security scanning in CI

The CI workflow runs OWASP Dependency-Check (`dependency-scan` job) and Trivy image scans (inside `docker-build`). Without an NVD API key, OWASP DC throttles severely (~30+ minutes per run) because the public NVD endpoint rate-limits unauthenticated requests.

To speed up `dependency-scan`:
1. Request a key at <https://nvd.nist.gov/developers/request-an-api-key>.
2. Add it to repo secrets as `NVD_API_KEY` (Settings → Secrets and variables → Actions).
3. Subsequent runs read the cached vulnerability database from `~/.gradle/dependency-check-data` and complete in 1–2 minutes.

Trivy and SARIF uploads (under Security → Code scanning) require no secret — `GITHUB_TOKEN` is enough. If a scan fails the job, inspect the SARIF in the Code scanning UI to decide whether to fix, suppress in `backend/dependency-check-suppressions.xml`, or accept (Trivy: add to `.trivyignore`).
