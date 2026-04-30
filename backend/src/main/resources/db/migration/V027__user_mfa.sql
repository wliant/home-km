-- TOTP MFA per RFC 6238. mfa_secret is base32-encoded; null until enrolled.
-- mfa_enabled stays false until the user verifies their first code, so a
-- half-finished enrollment never blocks login.
ALTER TABLE users
    ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN mfa_secret  VARCHAR(64) NULL;

-- Single-use backup codes for when the authenticator app is unavailable.
-- Stored as bcrypt hashes; consumed by setting used_at.
CREATE TABLE user_mfa_recovery_codes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash  VARCHAR(72) NOT NULL,
    used_at    TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mfa_recovery_user
    ON user_mfa_recovery_codes(user_id)
    WHERE used_at IS NULL;
