# ADR-0006: Encryption at rest — disk-level (LUKS), defer DB/MinIO column encryption

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-05-02 |
| Deciders | engineering |

## Context

Home KM stores household notes (some intentionally private) and binary files in PostgreSQL + MinIO. Our P1 backlog flagged "encryption at rest" as a gap, but the term covers very different layers:

1. **Disk-level (LVM/LUKS, ZFS, APFS encrypted volumes)** — the OS kernel encrypts every block under `/var/lib/{postgresql,minio}`. Transparent to the application, defeats only "physical theft of the disk" attacker.
2. **DB column encryption (`pgcrypto`)** — sensitive note bodies / files re-encrypted with a key managed by the app. Defeats "DBA browses the database" attacker.
3. **MinIO server-side encryption (SSE-S3 / SSE-KMS)** — MinIO encrypts objects with a per-object DEK derived from a KMS-held master. Defeats "operator copies the bucket" attacker.
4. **Client-side encryption** — content encrypted in the browser before upload. Defeats the server entirely; rules out server-side search and previews.

A self-hosted household app for 2–6 members lives in a small threat model: most deployments run on a NUC or VPS the same person who created the data also operates. The "DBA snooping" and "operator copying buckets" attackers don't really exist — they're the same person. The realistic loss vector is **disk theft / disposal**: a host migrates, an operator dumps a backup to an unencrypted laptop, or a drive is RMA'd without secure-erase.

We also have to weigh dependency budget. Per [`specs/00-overview.md`](../00-overview.md) we keep moving parts minimal. Adding `pgcrypto` per-column encryption pulls in a key-management story (rotation, backup, disaster recovery) that doesn't exist today; SSE-KMS pulls in either Vault or an external KMS sidecar. Either is more apparatus than what the threat model warrants.

## Decision

Document and recommend **disk-level encryption** as the supported encryption-at-rest posture for v1:

- Operator runbook ([`docs/RUNBOOK.md`](../../docs/RUNBOOK.md)) gains an "Encryption at rest" section with concrete instructions for LUKS on Linux and APFS encrypted volumes on macOS hosts, plus a note on encrypted EBS / GCE persistent disks for cloud deployments.
- Backup runbook ([`docs/backups.md`](../../docs/backups.md)) gains a paragraph requiring backup target encryption (`age`, `gpg`, or encrypted destination) so the cleartext data doesn't escape via the backup path.
- `.env.example` and `specs/12-infrastructure.md` get a one-line note pointing operators at the runbook section.
- No code change in v1.

DB column encryption (`pgcrypto`) and MinIO SSE remain **future work**, gated by:
- A demonstrated household need (e.g. a deployment where the operator and the data owner are not the same person).
- A concrete KMS choice (Vault vs HashiCorp / cloud KMS vs sealed-box file). We will not introduce one speculatively.

Client-side encryption is **out of scope** for the foreseeable future — it breaks server-side search (FTS + pgvector), thumbnails, previews, and the whole point of the smart-search feature shipped this quarter.

## Consequences

- **Positive:** No new dependencies, no key-rotation procedure to maintain, no risk of permanent data loss from a misplaced key. Threat model "physical disk theft" is covered with a documented, well-understood mechanism. Operators who care more (e.g. cloud deployments) can plug in stronger storage encryption (LUKS-on-LVM, encrypted EBS) without touching app code.
- **Negative:** Anyone with shell access on the running host sees cleartext. Backups are only as encrypted as the operator makes them — we rely on the runbook being read. A hostile DBA can read any note body with a `SELECT`.
- **Neutral:** The `embedding`, `tsvector`, and search indexes stay queryable (they would not survive column or client-side encryption without significant index-aware crypto work).

## Alternatives considered

- **`pgcrypto` per-column encryption with app-managed keys.** Solves the DBA-attacker case but the household threat model doesn't include them, and key rotation/backup/disaster-recovery is a real runbook to maintain. Defer.
- **MinIO SSE-S3 / SSE-KMS.** Same trade — no plausible attacker today, plus a KMS dependency. Defer; revisit if we ship a managed offering.
- **Client-side encryption (e.g. age-encrypt note bodies before POST).** Kills semantic search, kills thumbnails, kills `ts_rank` ordering. Would also blow up the browser bundle and the IndexedDB offline cache. Rejected for v1.
- **No documentation, "encryption is the operator's problem".** Rejected — operators routinely deploy on plain ext4 without realizing the implication; a runbook section is a near-zero-cost mitigation.

## References

- [`gaps/security/encryption-at-rest.md`](../../gaps/security/encryption-at-rest.md) — the gap entry this ADR closes.
- [`docs/RUNBOOK.md`](../../docs/RUNBOOK.md) — runbook, gets the new section.
- [`docs/backups.md`](../../docs/backups.md) — paired update for backup-target encryption.
- [`specs/00-overview.md`](../00-overview.md) — threat scope and dependency posture.
- ADR-0003 (MinIO) — explains why we're not on a managed KMS-equipped object store.
