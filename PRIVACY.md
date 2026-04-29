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
- Use Settings → Privacy → "Clear local data" to wipe what the browser remembers and start fresh.
