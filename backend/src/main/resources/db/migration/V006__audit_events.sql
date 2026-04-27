CREATE TABLE audit_events (
    id            BIGSERIAL    PRIMARY KEY,
    actor_user_id BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    action        VARCHAR(80)  NOT NULL,
    target_type   VARCHAR(40)  NULL,
    target_id     VARCHAR(40)  NULL,
    before_state  JSONB        NULL,
    after_state   JSONB        NULL,
    ip            VARCHAR(45)  NULL,
    user_agent    TEXT         NULL,
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_events_actor    ON audit_events(actor_user_id);
CREATE INDEX idx_audit_events_occurred ON audit_events(occurred_at DESC);
CREATE INDEX idx_audit_events_action   ON audit_events(action);
