# Encryption-at-rest

Home KM stores three classes of secret data:

| Where | What | Recommended posture |
|-------|------|---------------------|
| PostgreSQL | User credentials (bcrypt), audit history, refresh tokens (hashed), password reset tokens (hashed), invitations | Encrypt the **filesystem** the volume sits on. SQL data is not application-encrypted. |
| MinIO | All user-uploaded files | Enable MinIO server-side encryption (SSE-S3) — see below. |
| Backups | All of the above, off-host | Restic encrypts at the repository level by default; **do not lose `RESTIC_PASSWORD`**. |

## Operator-level: full-disk encryption

This is the strongest layer and the only thing that protects data at rest if a disk leaves the host. Use what your platform offers:

- **Linux**: LUKS at install time.
- **macOS**: FileVault.
- **Cloud volumes**: enable platform encryption (EBS, GCE PD, Azure Disk).

The application does not detect or enforce this. Document it in your deploy notes.

## MinIO server-side encryption (SSE-S3)

To enable automatic encryption of all objects MinIO writes, set:

```yaml
# docker-compose.infra.yml > minio.environment
MINIO_KMS_AUTO_ENCRYPTION: "on"
MINIO_KMS_SECRET_KEY: "homekm-key:<base64-32-bytes>"
```

Generate the key once:
```bash
openssl rand -base64 32
```

Existing objects are not retroactively encrypted; the operator should rewrite them with `mc cp --recursive` to flush through the encryption pipeline, or accept that only new uploads after the switch are encrypted.

## Per-note client-side encryption (deferred)

True zero-knowledge encryption of note bodies would require key management in the browser (e.g., per-user-derived AES via WebCrypto, plus key sharing for household-shared notes). This is tracked under P2 and not implemented today. The current `visibility` ACL keeps content readable to authorized household members; it does not protect against a database read by the operator.

## Out of scope

- Field-level DB encryption.
- KMS integration (HSM-backed keys).
- TLS — handled by your reverse proxy. See your proxy's docs.
