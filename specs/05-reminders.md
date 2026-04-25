# Reminders

## 1. Data Model

See `01-data-model.md` Sections 6 and 7 for the full column definitions of `reminders` and `reminder_recipients`.

**Key semantics:**
- A reminder is a sub-resource of a note
- `push_sent = true` means the notification has been sent and will not be sent again
- For recurring reminders: after sending, `push_sent` is reset to `false` and `remind_at` is advanced by the recurrence interval
- For non-recurring reminders: after sending, `push_sent` stays `true` permanently
- If `reminder_recipients` has no rows for a reminder, the notification falls back to the note's `owner_id`

---

## 2. API Endpoints

All reminder endpoints are sub-resources of notes. The parent note must be visible to the calling user (child-safe rules apply).

---

### `GET /api/notes/{noteId}/reminders`

List all reminders for a note.

**Auth required:** Yes

**Response (200):**
```json
[
  {
    "id": 1,
    "noteId": 1,
    "remindAt": "2025-04-26T09:00:00Z",
    "recurrence": "weekly",
    "pushSent": false,
    "createdAt": "2025-04-25T10:30:00Z",
    "updatedAt": "2025-04-25T10:30:00Z",
    "recipients": [
      { "userId": 1, "displayName": "Jane" },
      { "userId": 2, "displayName": "Bob" }
    ]
  }
]
```

---

### `POST /api/notes/{noteId}/reminders`

Create a reminder for a note.

**Auth required:** Yes — adults or note owner (child, but children don't own notes — effectively adults only unless a child created the note)

**Request body:**
```json
{
  "remindAt": "2025-04-26T09:00:00Z",
  "recurrence": "weekly",
  "recipientUserIds": [1, 2]
}
```

**Validation:**
- `remindAt` — required; must be a valid ISO-8601 datetime with timezone; must be in the future (UTC); must be at most 5 years in the future (`400 REMIND_AT_TOO_FAR`)
- `recurrence` — optional; if provided must be one of `daily`, `weekly`, `monthly`, `yearly`; `400 INVALID_RECURRENCE` otherwise
- `recipientUserIds` — optional array of user IDs; if empty or omitted, defaults to `[noteOwnerId]`; all provided IDs must be active users (`400 INVALID_RECIPIENT` for unknown IDs)
- Maximum 10 reminders per note: `400 REMINDER_LIMIT_EXCEEDED`

**Response (201):** Created reminder object (see response shape above).

---

### `PUT /api/notes/{noteId}/reminders/{reminderId}`

Update a reminder.

**Auth required:** Yes — adults or note owner (child)

**Request body:** Same as POST.

**Rules:**
- Updating `remindAt` or `recurrence` resets `push_sent = false` so the reminder fires again

**Response (200):** Updated reminder object.

---

### `DELETE /api/notes/{noteId}/reminders/{reminderId}`

Delete a reminder.

**Auth required:** Yes — adult only; child cannot delete

**Response (204):** No content.

---

## 3. Scheduler Implementation

Spring `@Scheduled(fixedRate = 60000)` — runs every 60 seconds.

**Query:**
```sql
SELECT r.*, n.owner_id AS note_owner_id, n.title AS note_title
FROM reminders r
JOIN notes n ON r.note_id = n.id
WHERE r.push_sent = FALSE AND r.remind_at <= NOW()
```

**For each result:**

1. Determine recipients:
   - Fetch `reminder_recipients` for this reminder
   - If empty: use `[note_owner_id]`
   - Collect `push_subscriptions` for all recipient user IDs

2. Send Web Push notification to each subscription endpoint (see Section 4)

3. Update reminder state:
   - If `recurrence IS NULL`: set `push_sent = true`
   - If `recurrence = 'daily'`: advance `remind_at += 1 day`, set `push_sent = false`
   - If `recurrence = 'weekly'`: advance `remind_at += 7 days`, set `push_sent = false`
   - If `recurrence = 'monthly'`: advance `remind_at` by 1 calendar month (use `remind_at + INTERVAL '1 month'`), set `push_sent = false`
   - If `recurrence = 'yearly'`: advance `remind_at` by 1 calendar year, set `push_sent = false`

4. On push delivery failure (HTTP 410 Gone from push service — endpoint no longer valid): delete the `push_subscriptions` row for that endpoint. Do not fail the scheduler tick.

5. On any other push delivery failure: log at WARN level; do not delete the subscription; the scheduler will retry on the next tick if `push_sent` was not set.

**Important:** The scheduler must be idempotent. If it crashes mid-run, reminders that were partially processed will be retried on the next tick (since `push_sent` is only set to `true` after successful delivery).

---

## 4. Web Push Implementation

**Library:** `nl.martijndwars:web-push` (Java VAPID implementation)

**Configuration (from env vars):**
| Env Var | Description |
|---------|------------|
| `VAPID_PUBLIC_KEY` | VAPID public key, base64url-encoded |
| `VAPID_PRIVATE_KEY` | VAPID private key, base64url-encoded |
| `VAPID_SUBJECT` | Must be a `mailto:` URI, e.g. `mailto:admin@example.com` |

**Notification payload JSON:**
```json
{
  "title": "${APP_NAME}",
  "body": "Reminder: {note title}",
  "icon": "/icons/icon-192.png",
  "data": {
    "noteId": 1,
    "noteTitle": "string"
  }
}
```

Where `${APP_NAME}` is the value of the `APP_NAME` environment variable.

**PushService bean:** Singleton Spring bean. Initialised with VAPID keys at startup. Throws on misconfigured keys — Spring context fails to start if VAPID keys are invalid.

---

## 5. Push Subscription Management

### `GET /api/vapid-public-key`

**Auth required:** No

Returns the VAPID public key for the frontend to use when creating a push subscription.

**Response (200):**
```json
{ "publicKey": "BNb4...base64url..." }
```

---

### `POST /api/push/subscribe`

Register a browser push subscription.

**Auth required:** Yes (any user including children)

**Request body** (the `PushSubscription` object from the browser's Push API):
```json
{
  "endpoint": "https://fcm.googleapis.com/fcm/send/...",
  "keys": {
    "p256dh": "base64url...",
    "auth": "base64url..."
  }
}
```

**Behaviour:** Upsert on `endpoint` — if the endpoint already exists (for any user), update `p256dh_key`, `auth_key`, and `user_id` to the current user. Returns `201` on new subscription, `200` on update.

---

### `DELETE /api/push/subscribe`

Remove a push subscription.

**Auth required:** Yes

**Request body:**
```json
{ "endpoint": "string" }
```

Only removes the subscription if it belongs to the current user. Returns `204` even if not found (idempotent).

---

## 6. Validation Rules Summary

| Constraint | Rule | Error Code |
|-----------|------|-----------|
| `remindAt` timezone | Must include Z or offset | `VALIDATION_ERROR` |
| `remindAt` future | Must be after `now()` UTC | `VALIDATION_ERROR` |
| `remindAt` max | At most 5 years from now | `REMIND_AT_TOO_FAR` |
| `recurrence` values | `daily`, `weekly`, `monthly`, `yearly`, or null | `INVALID_RECURRENCE` |
| Max reminders | 10 per note | `REMINDER_LIMIT_EXCEEDED` |
| Recipients | All IDs must be active users | `INVALID_RECIPIENT` |
