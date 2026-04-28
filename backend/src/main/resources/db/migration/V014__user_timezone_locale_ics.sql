ALTER TABLE users
    ADD COLUMN timezone   VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    ADD COLUMN locale     VARCHAR(16)  NOT NULL DEFAULT 'en',
    ADD COLUMN ics_token  VARCHAR(64)  NULL UNIQUE;

CREATE INDEX idx_users_ics_token ON users(ics_token);
