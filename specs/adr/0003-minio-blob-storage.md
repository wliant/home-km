# ADR-0003: MinIO over filesystem for blob storage

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-04-26 |
| Deciders | @wliant |

## Context

Files (images, PDFs, documents) need durable storage with three properties:

1. **Presigned URLs** — the API issues a short-lived URL the browser fetches directly, so backend bandwidth doesn't double-handle large blobs.
2. **Versioning + replication** for backups (`gaps/backup-dr/minio-backups.md`).
3. **Object semantics** — `PUT key`, `GET key`, `DELETE key`, `LIST prefix`. Filesystem can fake these but breaks down on rename, partial writes, and concurrent access without a lock manager.

The alternatives are (a) plain filesystem under `/var/lib/homekm/files/`, (b) S3 / R2 / B2, (c) MinIO running locally and speaking the S3 protocol.

## Decision

MinIO ships in `docker-compose.infra.yml`. The backend talks to it via the AWS S3 SDK shape (specifically the official `io.minio:minio` Java client). The bucket name is configurable (`MINIO_BUCKET_NAME`).

## Consequences

- **Positive:** Presigned URLs for 15 minutes (configurable via `PRESIGNED_URL_EXPIRY_MINUTES`), making the upload/download path bypass the API for the actual blob bytes. The same code can point at AWS S3, Backblaze B2, or any S3-compatible object store by changing `MINIO_ENDPOINT` — operators with cloud storage budgets get a one-line swap. Mirror-to-cold-storage (`mc mirror`) is a documented backup strategy (`docs/backups.md`).
- **Negative:** One more container to run. MinIO is JVM-heavy (~200MB RAM at idle) — non-trivial for a low-end NAS.
- **Neutral:** The presigned-URL pattern means the browser must reach the MinIO origin directly. CSP `media-src` / `img-src` / `frame-src` include `${MINIO_PUBLIC_ORIGIN}` to allow this.

## Alternatives considered

- **Plain filesystem.** Rejected — the API would sit in the read/write path of every download, inflating bandwidth and CPU. Backups become "rsync the directory" which can't easily be replicated to S3-compatible cold storage without re-implementing what `mc` already does.
- **AWS S3 / B2 directly.** Rejected at v1 because self-hosted households should not be required to have a cloud account. MinIO running on the NAS gives the same protocol locally, and operators who *want* cloud can flip the endpoint.

## References

- `specs/06-files.md`
- `backend/src/main/java/com/homekm/file/`
- `docs/backups.md`
