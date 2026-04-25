-- users
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

-- folders
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

CREATE UNIQUE INDEX folders_sibling_name_unique ON folders (COALESCE(parent_id, 0), name);
CREATE INDEX folders_parent_id_idx ON folders (parent_id);
CREATE INDEX folders_owner_id_idx ON folders (owner_id);
CREATE INDEX folders_search_vector_idx ON folders USING GIN (search_vector);

-- notes
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
    embedding     vector(1536)
);

CREATE INDEX notes_folder_id_idx ON notes (folder_id);
CREATE INDEX notes_owner_id_idx ON notes (owner_id);
CREATE INDEX notes_label_idx ON notes (label);
CREATE INDEX notes_search_vector_idx ON notes USING GIN (search_vector);

-- checklist_items
CREATE TABLE checklist_items (
    id         BIGSERIAL PRIMARY KEY,
    note_id    BIGINT   NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    text       TEXT     NOT NULL,
    is_checked BOOLEAN  NOT NULL DEFAULT FALSE,
    sort_order INTEGER  NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX checklist_items_note_id_sort_order_idx ON checklist_items (note_id, sort_order);

-- reminders
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

CREATE INDEX reminders_scheduler_idx ON reminders (push_sent, remind_at)
    WHERE push_sent = FALSE;

-- reminder_recipients
CREATE TABLE reminder_recipients (
    reminder_id BIGINT NOT NULL REFERENCES reminders(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (reminder_id, user_id)
);

CREATE INDEX reminder_recipients_user_id_idx ON reminder_recipients (user_id);

-- files
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
    embedding        vector(1536)
);

CREATE INDEX files_folder_id_idx ON files (folder_id);
CREATE INDEX files_owner_id_idx ON files (owner_id);
CREATE UNIQUE INDEX files_client_upload_id_idx ON files (owner_id, client_upload_id)
    WHERE client_upload_id IS NOT NULL;
CREATE INDEX files_search_vector_idx ON files USING GIN (search_vector);

-- tags
CREATE TABLE tags (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(7)   NOT NULL DEFAULT '#6366f1',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT tags_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE UNIQUE INDEX tags_name_lower_unique ON tags (LOWER(name));
CREATE INDEX tags_name_trgm_idx ON tags USING GIN (name gin_trgm_ops);

-- taggings
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

CREATE INDEX taggings_entity_idx ON taggings (entity_type, entity_id);
CREATE INDEX taggings_tag_id_idx ON taggings (tag_id);

-- push_subscriptions
CREATE TABLE push_subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint   TEXT         NOT NULL UNIQUE,
    p256dh_key TEXT         NOT NULL,
    auth_key   TEXT         NOT NULL,
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX push_subscriptions_user_id_idx ON push_subscriptions (user_id);

-- tsvector triggers: folders
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

-- tsvector triggers: notes
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

-- tsvector triggers: files
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
