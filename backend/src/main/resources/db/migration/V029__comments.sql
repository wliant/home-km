-- Comments live alongside notes and files. item_type narrows which table
-- item_id refers to; we keep them in one comments table so the read path is
-- a single index lookup regardless of attachment.
CREATE TABLE comments (
    id          BIGSERIAL PRIMARY KEY,
    item_type   VARCHAR(16)  NOT NULL CHECK (item_type IN ('note', 'file')),
    item_id     BIGINT       NOT NULL,
    author_id   BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body        TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    edited_at   TIMESTAMPTZ  NULL,
    deleted_at  TIMESTAMPTZ  NULL
);

CREATE INDEX idx_comments_item ON comments(item_type, item_id, created_at)
    WHERE deleted_at IS NULL;

-- Each row mentions one user. Group mentions are expanded at write time.
CREATE TABLE comment_mentions (
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    PRIMARY KEY (comment_id, user_id)
);

-- Per-user inbox row for "you were mentioned, go look". read_at flips when
-- the user opens the source comment.
CREATE TABLE mention_inbox (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment_id BIGINT      NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    read_at    TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mention_inbox_unread
    ON mention_inbox(user_id, created_at DESC)
    WHERE read_at IS NULL;

CREATE UNIQUE INDEX idx_mention_inbox_unique
    ON mention_inbox(user_id, comment_id);
