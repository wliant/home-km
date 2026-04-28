ALTER TABLE notes   ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'household';
ALTER TABLE files   ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'household';
ALTER TABLE folders ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'household';

CREATE TABLE item_acls (
    id          BIGSERIAL    PRIMARY KEY,
    item_type   VARCHAR(16)  NOT NULL,
    item_id     BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(16)  NOT NULL DEFAULT 'VIEWER',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (item_type, item_id, user_id)
);
CREATE INDEX idx_item_acls_item ON item_acls(item_type, item_id);
CREATE INDEX idx_item_acls_user ON item_acls(user_id);
