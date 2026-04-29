# On-call playbook

Symptom-first guide for operators of a Home KM deployment. Each entry is **what you see → what's likely broken → how to fix it**. Keep entries short; long-form context belongs in `docs/RUNBOOK.md`.

If you're paging here cold, start with:

```bash
docker compose -f docker-compose.app.yml ps
docker compose -f docker-compose.app.yml logs --tail=200 api
docker compose -f docker-compose.infra.yml ps
curl -fsS localhost:8080/actuator/health
curl -fsS localhost:8080/api/info       # version + commit
```

That covers ~70% of incidents.

---

## API returns 5xx

**Diagnose**

```bash
docker compose -f docker-compose.app.yml logs --tail=200 api | grep -E '"level":"ERROR"'
curl -fsS localhost:8080/actuator/health/readiness | jq .
```

Look for the latest stack trace. The structured-JSON logs include `requestId` — match it with the failing client request to pin the offending call.

**Common causes**

| Trigger | Fix |
|---------|-----|
| `STORAGE_UNAVAILABLE` (503) | MinIO circuit breaker is open. See "Uploads / downloads fail" below. |
| `JWT signature does not match` | `JWT_SECRET` was rotated without restarting clients. Restart the API; users will be forced to sign in again. |
| `cannot acquire connection` (Hikari) | DB is unreachable or pool is saturated. Check `docker compose ... ps postgres` and `actuator/health/readiness`. Bump `DB_POOL_SIZE` if saturation is sustained. |
| `Migration failed` on startup | Flyway found a checksum mismatch — somebody edited a `V*.sql` file after deploy. Restore the original or write a forward-rolling fix migration. |
| Random 500 with `JsonMappingException` | DTO drift between frontend and backend. The `OpenApiContractTest` runs in CI to catch this; if it slipped through, regenerate the baseline (`UPDATE_OPENAPI_BASELINE=1 ./gradlew test --tests *OpenApiContractTest`) and reconcile. |

---

## Uploads / downloads fail

**Diagnose**

```bash
docker compose -f docker-compose.infra.yml logs --tail=100 minio
curl -fsS localhost:8080/actuator/health/minio
```

**Common causes**

| Symptom | Fix |
|---------|-----|
| 503 `STORAGE_UNAVAILABLE` | Circuit breaker opened. Wait 30s for half-open or restart the API to force-reset. Confirm MinIO is healthy first. |
| Slow upload then 504 | Frontend nginx `proxy_send_timeout` (300s) hit. Check `MAX_FILE_UPLOAD_MB` and the actual blob size. |
| 403 from presigned URL | `MINIO_PUBLIC_ORIGIN` mismatches the browser-visible URL. Edit `.env` and rebuild the frontend. |
| `BucketDoesNotExist` | Bucket was wiped from MinIO but `MINIO_BUCKET_NAME` still references it. Re-create with `mc mb`. |

---

## Push notifications stop firing

**Diagnose**

```bash
docker compose logs api | grep "Push delivery failed"
docker compose logs api | grep "VAPID keys not configured"
```

**Common causes**

| Symptom | Fix |
|---------|-----|
| `VAPID keys not configured` | `VAPID_PUBLIC_KEY` / `_PRIVATE_KEY` missing from `.env`. Run `scripts/gen-vapid-keys.sh`, paste output, restart API. |
| Repeated `410 Gone` from a single endpoint | The browser unsubscribed. The row in `push_subscriptions` is removed automatically. Nothing to do. |
| `notification_prefs.reminders=false` | Per-user opt-out (Settings → Push notifications). Not a bug — confirm with the user. |
| Reminders fire but the OS shows nothing | Browser/OS-level "Focus" or "Do not disturb" is on. Test with `POST /api/test/trigger-scheduler` (test profile only). |

---

## Login is broken

**Diagnose**

```bash
docker compose logs api | grep -E '(LoginRateLimiter|INVALID_CREDENTIALS|RATE_LIMITED)'
```

**Common causes**

| Symptom | Fix |
|---------|-----|
| 429 `RATE_LIMITED` for everyone | `LoginRateLimiter` is too tight. Restart the API to clear the in-memory counter; raise `app.rate-limit.login.*` if the limit is genuinely too low. |
| 401 with correct credentials | `JWT_SECRET` changed since the user's last sign-in (their refresh token still hashes against the old secret). Force a fresh sign-in or roll back the secret. |
| `is_active=false` | Account was self-deactivated (Settings → Danger Zone). Re-enable: `UPDATE users SET is_active = true WHERE email = ...`. |
| First-user bootstrap returned 403 | An admin already exists. The `first user is admin` rule only fires when the table is empty. |

---

## CORS / Origin errors

The API rejects requests with non-matching `Origin` with 403. Check the browser console: the failing origin must be in the comma-separated `CORS_ALLOWED_ORIGINS` env. LAN access via IP needs the IP listed; access via hostname needs the hostname. After editing `.env`, restart the API.

---

## Disk fills up

```bash
df -h
du -sh /var/lib/docker/volumes/*
docker system df -v
```

**Likely culprits**

- **Postgres WAL** without archiving — check `docker volume inspect homekm_pgdata`. If WAL is the problem, set `archive_mode=off` for testing or wire WAL archiving per `docs/backups.md`.
- **MinIO miniodata volume** filled with thumbnails. Run a cleanup: `mc rm --recursive --force homekm/homekm/<userId>/_thumb*` is destructive — use the admin "Trash purge" path instead.
- **Container logs** — log retention caps are documented in RUNBOOK § "Container log retention"; the per-driver caps cap each service. Logs above the cap are dropped automatically.

---

## "Everything looks fine but the UI is broken"

```bash
curl -fsS localhost:8080/api/info       # what's running
curl -fsS localhost:3000/                # frontend up?
docker compose logs frontend | tail -20
```

**Frontend** ships as a static bundle behind nginx; if `/api/info` works but the SPA shell doesn't load, the issue is the nginx layer. Check `docker compose -f docker-compose.app.yml ps frontend` and `docker logs <frontend-container>`. Most "broken UI" reports trace to a stale browser cache — the service worker holds onto the old shell. Clearing local data (Settings → Privacy) or hard-reloading (Cmd-Shift-R) fixes it.

**Build/version** mismatch: ask the user to copy the Settings → About block (version, commit, branch). Compare with `git log` to see what they're actually running.

---

## When in doubt

1. **Restart the API** (`docker compose -f docker-compose.app.yml restart api`). Resets in-memory rate limits, circuit breakers, and JWT denylist. Won't lose data.
2. **Run the restore drill** dry-run if you suspect data corruption (`docs/restore-drill.md`).
3. **Roll back** to the previous Docker tag — release-please tags every release; redeploy a known-good `vX.Y.Z` and ask the user to retry. Cosign-verify the image first if you're feeling spicy: `cosign verify ghcr.io/wliant/home-km/api:vX.Y.Z` (recipe in RUNBOOK).

Add a new entry here every time an incident teaches you something. Stale playbooks are worse than no playbook.
