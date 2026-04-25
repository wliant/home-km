# Data Model

## 1. Database Engine and Extensions

**Engine:** PostgreSQL 15

**Required extensions** (run at database init, before any migrations):
```sql
CREATE EXTENSION IF NOT EXISTS pgvector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

These are installed via `./infra/postgres/init.sql`, mounted as a Docker init script. See `12-infrastructure.md`.

**Migration tool:** Flyway. Migration files live at `backend/src/main/resources/db/migration/`. Naming convention: `V{NNN}__{description}.sql` where NNN is zero-padded to 3 digits.

- `V001__init.sql` — all tables defined in this spec
- `V002__...sql` — future additions

Flyway runs automatically on Spring Boot startup. A failed migration prevents the application from starting.

---

## 2. Table: `users`

```sql
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(320)  NOT NULL UNIQUE,
    password_hash VARCHAR(72)   NOT NULL,
    display_name  VARCHAR(100)  NOT NULL,
    is_admin      BOOLEAN       NOT NULL DEFAULT FALSE,
    is_child      BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT users_not_admin_and_child CHECK (NOT (is_admin AND is_child))
);
```

**Column notes:**
- `password_hash` — bcrypt output (always 60 chars); 72 chars for headroom
- `is_admin` — grants access to admin endpoints
- `is_child` — restricts to child-safe content and write operations; mutually exclusive with `is_admin`
- `is_active` — soft-disable; inactive accounts cannot login

**Indexes:**
- Unique index on `email` (covered by UNIQUE constraint)

---

## 3. Table: `folders`

```sql
CREATE TABLE folders (
    id            BIGSERIAL PRIMARY KEY,
    parent_id     BIGINT        REFERENCES folders(id) ON DELETE RESTRICT,
    name          VARCHAR(255)  NOT NULL,
    description   TEXT,
    owner_id      BIGINT        NOT NULL REFERENCES users(id),
    is_child_safe BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    search_vector TSVECTOR
);
```

**Column notes:**
- `parent_id = NULL` — root-level folder
- `search_vector` — maintained by trigger (see Section 11)

**Constraints:**
- Sibling name uniqueness: `CREATE UNIQUE INDEX folders_sibling_name_unique ON folders (COALESCE(parent_id, 0), name);`
- Cycle prevention: enforced in application logic via recursive CTE before insert/update (not a DB constraint)

**Indexes:**
```sql
CREATE INDEX folders_parent_id_idx ON folders (parent_id);
CREATE INDEX folders_owner_id_idx ON folders (owner_id);
CREATE INDEX folders_search_vector_idx ON folders USING GIN (search_vector);
```

---

## 4. Table: `notes`

```sql
CREATE TABLE notes (
    id            BIGSERIAL PRIMARY KEY,
    folder_id     BIGINT        REFERENCES folders(id) ON DELETE SET NULL,
    owner_id      BIGINT        NOT NULL REFERENCES users(id),
    title         VARCHAR(500)  NOT NULL,
    body          TEXT,
    label         VARCHAR(50)   NOT NULL DEFAULT 'custom',
    is_child_safe BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    search_vector TSVECTOR,
    embedding     VECTOR(1536)
);
```

**Column notes:**
- `folder_id = NULL` — note is at virtual root level
- `label` — enforced as enum at application layer (see Section 4a); stored as VARCHAR for flexibility
- `body` — raw Markdown, max 100,000 characters (enforced by application layer)
- `embedding` — nullable; reserved for future semantic search; no application code reads or writes this in v1; no index in v1
- `search_vector` — maintained by trigger

**Label enum values** (enforced by Spring `@ValidLabel`, stored as VARCHAR):
`recipe`, `todo`, `reminder`, `shopping_list`, `home_items`, `usage_manual`, `goal`, `aspiration`, `wish_list`, `travel_log`, `custom`

**Indexes:**
```sql
CREATE INDEX notes_folder_id_idx ON notes (folder_id);
CREATE INDEX notes_owner_id_idx ON notes (owner_id);
CREATE INDEX notes_label_idx ON notes (label);
CREATE INDEX notes_search_vector_idx ON notes USING GIN (search_vector);
```

---

## 5. Table: `checklist_items`

```sql
CREATE TABLE checklist_items (
    id         BIGSERIAL PRIMARY KEY,
    note_id    BIGINT   NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    text       TEXT     NOT NULL,
    is_checked BOOLEAN  NOT NULL DEFAULT FALSE,
    sort_order INTEGER  NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Indexes:**
```sql
CREATE INDEX checklist_items_note_id_sort_order_idx ON checklist_items (note_id, sort_order);
```

---

## 6. Table: `reminders`

```sql
CREATE TABLE reminders (
    id          BIGSERIAL PRIMARY KEY,
    note_id     BIGINT      NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    remind_at   TIMESTAMPTZ NOT NULL,
    recurrence  VARCHAR(20),
    push_sent   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reminders_recurrence_check
        CHECK (recurrence IS NULL OR recurrence IN ('daily', 'weekly', 'monthly', 'yearly'))
);
```

**Column notes:**
- `push_sent = TRUE` — scheduler will not send again (unless recurrence advances `remind_at`)
- `recurrence = NULL` — one-shot reminder

**Indexes:**
```sql
CREATE INDEX reminders_scheduler_idx ON reminders (push_sent, remind_at)
    WHERE push_sent = FALSE;
```

---

## 7. Table: `reminder_recipients`

```sql
CREATE TABLE reminder_recipients (
    reminder_id BIGINT NOT NULL REFERENCES reminders(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (reminder_id, user_id)
);
```

**Notes:** Defines which household members receive a push notification for a given reminder. If the table has no rows for a reminder, only the note owner is notified (fallback behaviour).

**Indexes:**
```sql
CREATE INDEX reminder_recipients_user_id_idx ON reminder_recipients (user_id);
```

---

## 8. Table: `files`

```sql
CREATE TABLE files (
    id               BIGSERIAL PRIMARY KEY,
    folder_id        BIGINT        REFERENCES folders(id) ON DELETE SET NULL,
    owner_id         BIGINT        NOT NULL REFERENCES users(id),
    filename         VARCHAR(500)  NOT NULL,
    mime_type        VARCHAR(127)  NOT NULL,
    size_bytes       BIGINT        NOT NULL,
    minio_key        VARCHAR(1000) NOT NULL UNIQUE,
    thumbnail_key    VARCHAR(1000),
    description      TEXT,
    is_child_safe    BOOLEAN       NOT NULL DEFAULT FALSE,
    client_upload_id VARCHAR(36),
    uploaded_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    search_vector    TSVECTOR,
    embedding        VECTOR(1536)
);
```

**Column notes:**
- `folder_id = NULL` — file is at virtual root level
- `minio_key` — full object path in MinIO (see `06-files.md` Section 1)
- `thumbnail_key` — MinIO key for 256×256 JPEG thumbnail; null if not an image or generation failed
- `client_upload_id` — UUID provided by client for idempotent offline uploads; unique per user
- `embedding` — nullable; reserved for future semantic search; no index in v1

**Indexes:**
```sql
CREATE INDEX files_folder_id_idx ON files (folder_id);
CREATE INDEX files_owner_id_idx ON files (owner_id);
CREATE UNIQUE INDEX files_client_upload_id_idx ON files (owner_id, client_upload_id)
    WHERE client_upload_id IS NOT NULL;
CREATE INDEX files_search_vector_idx ON files USING GIN (search_vector);
```

---

## 9. Table: `tags`

```sql
CREATE TABLE tags (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(7)   NOT NULL DEFAULT '#6366f1',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT tags_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);
```

**Indexes:**
```sql
CREATE UNIQUE INDEX tags_name_lower_unique ON tags (LOWER(name));
CREATE INDEX tags_name_trgm_idx ON tags USING GIN (name gin_trgm_ops);
```

---

## 10. Table: `taggings`

Polymorphic join table connecting tags to notes, files, or folders.

```sql
CREATE TABLE taggings (
    id          BIGSERIAL PRIMARY KEY,
    tag_id      BIGINT      NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    entity_type VARCHAR(20) NOT NULL,
    entity_id   BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT taggings_entity_type_check
        CHECK (entity_type IN ('note', 'file', 'folder')),
    CONSTRAINT taggings_unique
        UNIQUE (tag_id, entity_type, entity_id)
);
```

**Indexes:**
```sql
CREATE INDEX taggings_entity_idx ON taggings (entity_type, entity_id);
CREATE INDEX taggings_tag_id_idx ON taggings (tag_id);
```

---

## 11. Table: `push_subscriptions`

```sql
CREATE TABLE push_subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint   TEXT         NOT NULL UNIQUE,
    p256dh_key TEXT         NOT NULL,
    auth_key   TEXT         NOT NULL,
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

**Indexes:**
```sql
CREATE INDEX push_subscriptions_user_id_idx ON push_subscriptions (user_id);
```

---

## 12. tsvector Trigger Pattern

All three tables with `search_vector` columns use a `BEFORE INSERT OR UPDATE` trigger to maintain the tsvector.

**Folders:**
```sql
CREATE OR REPLACE FUNCTION folders_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('english',
        COALESCE(NEW.name, '') || ' ' || COALESCE(NEW.description, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER folders_search_vector_trigger
    BEFORE INSERT OR UPDATE OF name, description ON folders
    FOR EACH ROW EXECUTE FUNCTION folders_search_vector_update();
```

**Notes:**
```sql
CREATE OR REPLACE FUNCTION notes_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('english',
        COALESCE(NEW.title, '') || ' ' || COALESCE(NEW.body, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER notes_search_vector_trigger
    BEFORE INSERT OR UPDATE OF title, body ON notes
    FOR EACH ROW EXECUTE FUNCTION notes_search_vector_update();
```

**Files:**
```sql
CREATE OR REPLACE FUNCTION files_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('english',
        COALESCE(NEW.filename, '') || ' ' || COALESCE(NEW.description, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER files_search_vector_trigger
    BEFORE INSERT OR UPDATE OF filename, description ON files
    FOR EACH ROW EXECUTE FUNCTION files_search_vector_update();
```

---

## 13. Entity Relationship Overview

```
users ──────────────────────────────────────────────────────┐
  │ owns                                                     │
  ├── folders (parent_id self-ref tree)                      │
  │     └── folders (recursive)                              │
  ├── notes ──── checklist_items (CASCADE delete)            │
  │         └── reminders ──── reminder_recipients ──── users│
  ├── files                                                  │
  └── push_subscriptions                                     │
                                                             │
tags ──── taggings (entity_type + entity_id poly) ──── notes │
                                                    ├── files│
                                                    └── folders
```

---

## 14. Flyway Migration Naming Convention

```
V001__init.sql                    — all tables above
V002__add_push_subscriptions.sql  — if split later
V003__...sql
```

All tables from this spec must be created in `V001__init.sql` in dependency order:
1. `users`
2. `folders`
3. `notes`
4. `checklist_items`
5. `reminders`
6. `reminder_recipients`
7. `files`
8. `tags`
9. `taggings`
10. `push_subscriptions`
11. All trigger functions and triggers
