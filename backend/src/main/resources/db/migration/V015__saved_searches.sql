CREATE TABLE saved_searches (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    query       JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);

CREATE INDEX idx_saved_searches_user ON saved_searches(user_id, created_at DESC);
