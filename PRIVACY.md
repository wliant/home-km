# Privacy

Home KM is self-hosted: nothing leaves your server unless you configure it to. This document records what the app keeps about you, where, and for how long, so household members know what they're trusting the operator with.

## On the server

| Data class | Where | Retention |
|------------|-------|-----------|
| User account (email, hashed password, display name, role, timezone, locale) | Postgres `users` | Until the account is hard-deleted by an admin. |
| Notes (title, body, checklist items) | Postgres `notes`, `checklist_items` | Until trashed; trashed items purge after 30 days. |
| Files (binary + metadata) | MinIO + Postgres `files` | Until trashed; trashed items purge after 30 days. |
| Folders, tags, taggings | Postgres | Until deleted. |
| Reminders + recipients | Postgres `reminders`, `reminder_recipients` | Until the reminder fires (one-shot) or is deleted. |
| Push subscription endpoints | Postgres `push_subscriptions` | Until you disable push or unsubscribe at the OS level. Removed on `410 Gone` from the push service. |
| Refresh tokens (hashed) + device labels | Postgres `refresh_tokens` | Until you sign out, change password, or the token expires. |
| Audit log events | Postgres `audit_events` | One year by default — see RETENTION.md to change. |
| Saved searches (name + query JSON) | Postgres `saved_searches` | Until you delete them. |
| Notification preferences | `users.notification_prefs` JSON | Until you change them. |

The server never logs full request bodies. Structured logs include the `requestId`, `userId`, HTTP method, path, and status — bodies are redacted to keep passwords / tokens out of disk.

## On your device

The frontend runs entirely in your browser. It stores:

| What | Where | Why |
|------|-------|-----|
| JWT access token + refresh token | `localStorage` (`homekm-auth`) | Keeps you signed in across reloads. Cleared on sign-out. |
| Theme + accent preference | `localStorage` (`homekm-theme`) | Remembers your colour scheme. |
| Install banner dismissal timestamp + visit count | `localStorage` | Suppresses the install prompt for 14 days after dismissal. |
| Queued offline uploads (file blob + metadata) | IndexedDB (`homekm-offline-queue`) | Lets you upload while disconnected; flushed when you reconnect. |
| Cached note metadata (Dexie) | IndexedDB (`homekm-offline`) | Powers the offline read-only view. |
| Service-worker cache | Cache Storage | Speeds up app loads and serves the shell offline. |

Settings → Privacy lists the same data and offers a "Clear local data" button that signs you out and wipes everything above.

## Third parties

The default deploy talks to:

- **Your push gateway** (Apple, Google, Mozilla) when you opt in to push. Subscription endpoints are stored on your server, not shared with the operator.
- **Your SMTP relay** if email is configured (password reset, invitations).

Optional features can talk to more services if the operator opts in:

- Backups: an off-host target like Backblaze B2 or Hetzner Storage Box (`docs/backups.md`).
- Image scanning: an Ollama / OpenAI-compatible endpoint when `EMBEDDING_ENABLED=true`.
- Antivirus: a ClamAV daemon when `FILES_REQUIRE_SCAN=true`.
- TLS issuance: Let's Encrypt or Cloudflare (`docs/tls.md`).

The vanilla install reaches none of these.

## Your controls

- Sign out everywhere from Settings → Sessions.
- Disable reminder pushes from Settings → Push notifications.
- Deactivate your account from Settings → Danger zone (sign-in is blocked; an admin can re-enable inside any grace window).
- Ask an admin to hard-delete your data; the policy is documented in `RETENTION.md`.
- Request a portable export of your own data from Settings → Privacy → "Export my data". The server assembles a ZIP (notes as markdown, side data as JSON, manifest) and serves it via a 15-minute presigned URL; the request itself stays valid for 24h before the ZIP is purged.
- Use Settings → Privacy → "Clear local data" to wipe what the browser remembers and start fresh.

## GDPR mapping

Home KM is a household-scoped self-hosted app, so the GDPR concepts of "controller" and "processor" both fall on the operator. The features below provide the technical foundation if your jurisdiction requires data-subject-request handling — operators are expected to define a documented response process on top.

| Article | Right | How Home KM supports it |
|---|---|---|
| 15 | Access | `GET /api/auth/me` + the export ZIP (Article 20) cover the structured copy. |
| 16 | Rectification | All editable fields are exposed under Settings → Profile, Settings → Sessions, and the per-feature edit screens. |
| 17 | Erasure ("right to be forgotten") | Self-service deactivation blocks sign-in immediately; the operator hard-deletes the row out-of-band per `RETENTION.md`. Audit-log entries are retained for the configured window because the lawful basis is "legal obligation" (record-keeping). |
| 18 | Restriction | Deactivation freezes processing while leaving data intact for later restoration. |
| 20 | Portability | `POST /api/me/export` enqueues an asynchronous ZIP build; the user polls `GET /api/me/export/{id}` and downloads via a presigned URL when status flips to `READY`. Output is markdown + JSON, both portable open formats. |
| 21 | Objection | Push, email, and reminder routing are all per-user opt-in (`notification-prefs`). |

Default-on operators get all of the above by virtue of running the upstream app — no extra configuration needed.
