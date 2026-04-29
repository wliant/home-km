# Data retention

Each data class lives until the operator chooses to delete it. This document records the **defaults** so a household admin can answer "what does the system keep, and for how long?" without reading the schema.

Values are indicative — there is no automated enforcement beyond the trash purge job. Adjust per your jurisdiction or household preference and rerun the relevant cleanup script.

| Data class | Default retention | Configurable via | Cleanup mechanism |
|------------|-------------------|------------------|-------------------|
| Active notes / files / folders / tags | Indefinite | n/a | Manual delete |
| Trashed notes / files / folders | 30 days, then hard-purged | `app.trash.retention-days` (planned) | `TrashPurgeJob` (`@Scheduled`, daily) |
| Audit log events | 1 year | `app.audit.retention-days` | `AuditPurgeJob` (`@Scheduled`, weekly) |
| Refresh tokens | Until expired or explicitly revoked. "Remember me" defaults 30 days, "single-session" 8 hours. | `JWT_REFRESH_*_DAYS` env vars | `RefreshTokenPurgeJob` (`@Scheduled`, daily) |
| Idempotency keys | 24 hours | `app.idempotency.retention-hours` | `IdempotencyPurgeJob` (`@Scheduled`, hourly) |
| Push subscriptions | Until 410 Gone from the push gateway | n/a | `PushService` removes on 410 |
| Saved searches | Indefinite | User can delete from Search UI | n/a |
| Notification preferences | Indefinite | User edits from Settings | n/a |
| Backups (Postgres + MinIO) | 30 days local rotation, 90 days off-host | Restic policy in `docs/backups.md` | `restic forget` cron |
| Container logs (json-file driver) | ~250 MB rolling per service | `logging.options.max-size`/`max-file` in compose | Docker daemon |
| Application metrics (Prometheus) | 15 days | Prometheus `--storage.tsdb.retention.time` | Prometheus daemon |

## Soft delete vs hard delete

- **Soft delete** flips a `deleted_at` timestamp; the row stays in the table so the user can restore via Settings → Trash. After 30 days `TrashPurgeJob` runs `DELETE` for real and (for files) drops the MinIO object.
- **Hard delete** is what the admin runs on a deactivated account to wipe the user's owned content per the policy below. There is no undo.

## Deactivated accounts

A self-service deactivation (`Settings → Danger zone`) only flips `is_active=false`. The household admin is responsible for the policy that follows:

1. Wait the household-policy grace window (default: 7 days, unenforced — re-activation by flipping `is_active=true` is fine inside or outside this window).
2. Run `DELETE FROM users WHERE id = ? AND is_active = false` to cascade through the foreign keys (notes, files, reminders, refresh tokens, etc.). Items shared via per-item ACLs need re-assignment first per `gaps/sharing/per-item-acls.md`.
3. Drop MinIO objects under the user's prefix.

A future migration may automate this with a scheduled `UserPurgeJob`. Until then it is manual SQL on operator instruction.

## See also

- `PRIVACY.md` — what we keep about you and where.
- `docs/backups.md` — backup cadence and restic policy.
- `docs/RUNBOOK.md` § "Container log retention" — log driver caps.
