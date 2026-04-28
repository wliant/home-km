# Backups

The default deploy keeps backups on the same host as the data — convenient for development but unsafe in production. **Pair the local backups with an off-host target.**

## Recommended targets

| Target | Why | Caveat |
|--------|-----|--------|
| Backblaze B2 | Cheap, simple S3-compatible API | One-time setup of restic credentials |
| Hetzner Storage Box | EU-based, fixed price | SFTP only |
| Friend's NAS | Free if you have one | Trust + bandwidth |

## restic example (Backblaze B2)

```bash
# Initialize once (per repo)
export RESTIC_REPOSITORY="b2:homekm-backups:/"
export B2_ACCOUNT_ID="…"
export B2_ACCOUNT_KEY="…"
export RESTIC_PASSWORD="<long random>"   # store in a password manager
restic init

# Daily backup script (cron @daily)
docker compose -f docker-compose.infra.yml exec -T postgres \
    pg_dump -U "$DB_USER" "$DB_NAME" | \
    restic backup --stdin --stdin-filename "postgres.sql" --tag postgres

mc alias set homekm http://localhost:9000 "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY"
mc mirror --overwrite homekm/homekm /tmp/homekm-minio
restic backup /tmp/homekm-minio --tag minio

# Retention: 7 daily, 4 weekly, 12 monthly
restic forget --keep-daily 7 --keep-weekly 4 --keep-monthly 12 --prune
```

Schedule via cron:
```cron
@daily /usr/local/bin/homekm-backup.sh >> /var/log/homekm-backup.log 2>&1
```

## Hetzner Storage Box (SFTP)

```bash
export RESTIC_REPOSITORY="sftp:u123456@u123456.your-storagebox.de:/homekm"
restic init
# rest of pipeline identical
```

## Verifying

```bash
restic check         # verify repo integrity (slow but thorough)
restic snapshots     # list available snapshots
restic restore latest --target /tmp/restore --tag postgres
```

Then run the procedure in `docs/restore-drill.md`.

## What is NOT backed up

- `JWT_SECRET` and other env vars: store these separately in a password manager / secret vault.
- VAPID keys: regenerate from `scripts/` if lost (existing push subscriptions will need to re-subscribe).
- Image transforms: regenerated on demand from originals.
