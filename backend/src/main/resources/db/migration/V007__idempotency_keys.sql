CREATE TABLE idempotency_keys (
    id              BIGSERIAL    PRIMARY KEY,
    key_hash        VARCHAR(64)  NOT NULL,
    user_id         BIGINT       NULL REFERENCES users(id) ON DELETE CASCADE,
    method          VARCHAR(10)  NOT NULL,
    path            VARCHAR(500) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    status_code     INT          NOT NULL,
    response_body   TEXT         NULL,
    response_ct     VARCHAR(120) NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ  NOT NULL,
    UNIQUE (key_hash, user_id, method, path)
);
CREATE INDEX idx_idempotency_keys_expires ON idempotency_keys(expires_at);
