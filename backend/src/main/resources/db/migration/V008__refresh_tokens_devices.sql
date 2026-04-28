ALTER TABLE refresh_tokens
    ADD COLUMN device_label   VARCHAR(120) NULL,
    ADD COLUMN user_agent     VARCHAR(500) NULL,
    ADD COLUMN ip_address     VARCHAR(45)  NULL,
    ADD COLUMN last_seen_at   TIMESTAMPTZ  NULL,
    ADD COLUMN remember_me    BOOLEAN      NOT NULL DEFAULT FALSE;

CREATE INDEX idx_refresh_tokens_user_active
    ON refresh_tokens(user_id) WHERE revoked_at IS NULL;
