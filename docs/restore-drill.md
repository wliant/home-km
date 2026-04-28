# Restore drill

**Cadence:** monthly. Add a calendar reminder.
**Goal:** confirm we can restore Postgres + MinIO from the most recent backup, end-to-end, in under 30 minutes.

## Procedure

1. **Pick a backup.** Use the most recent successful backup (Postgres dump under `backups/`, MinIO mirror under `backups/minio/`, or restic snapshot per `docs/backups.md`).
2. **Spin up an isolated environment.**
   ```bash
   docker compose -f docker-compose.infra.yml -p homekm-drill up -d
   ```
3. **Restore Postgres:**
   ```bash
   docker compose -p homekm-drill -f docker-compose.infra.yml exec -T postgres \
     psql -U "$DB_USER" "$DB_NAME" < backups/homekm-YYYY-MM-DD.sql
   ```
4. **Restore MinIO:**
   ```bash
   mc alias set drill http://localhost:9000 "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY"
   mc mirror --overwrite backups/minio/ drill/homekm
   ```
5. **Boot the API against the drill instance:**
   ```bash
   docker compose -p homekm-drill -f docker-compose.app.yml up -d --build
   ```
6. **Verify:**
   - `curl -fsS http://localhost:8080/actuator/health/readiness` returns `UP`
   - Log in as a known user
   - Open one note from the canary set (canary list: see `scripts/restore-drill.sh`)
   - Download one file from the canary set
7. **Record results** in `docs/restore-drill-log.md`: date, restore duration, any anomalies, who ran it.
8. **Tear down:**
   ```bash
   docker compose -p homekm-drill -f docker-compose.app.yml down
   docker compose -p homekm-drill -f docker-compose.infra.yml down -v
   ```

## What to look for

- Does the restore complete cleanly, no constraint violations?
- Are MinIO objects accessible via presigned URLs?
- Are search results correct (full-text reindex needed?)
- Are checksums of canary files unchanged from the live system?

## When something fails

Open an issue tagged `backup-dr`. Block the next release until the restore is reproducible — silent backup rot is the most expensive class of incident.
