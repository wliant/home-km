CREATE TABLE invitations (
    id              BIGSERIAL    PRIMARY KEY,
    token_hash      VARCHAR(64)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(16)  NOT NULL DEFAULT 'USER',
    invited_by      BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    accepted_at     TIMESTAMPTZ  NULL,
    accepted_by     BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_invitations_email      ON invitations(email);
CREATE INDEX idx_invitations_expires_at ON invitations(expires_at);
