CREATE TABLE outbox_events (
    id            BIGSERIAL PRIMARY KEY,
    -- Stable type code (e.g. REMINDER_PUSH); future event types share the
    -- same publisher loop without schema changes.
    event_type    VARCHAR(64)  NOT NULL,
    payload       JSONB        NOT NULL,
    -- Fan-out: the publisher copies these into per-user push attempts.
    user_ids      BIGINT[]     NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- next_attempt_at < now() = ready to publish. Backoff bumps it on
    -- failure so the same poll doesn't replay the doomed event in a tight
    -- loop. Successful events get deleted; permanent failures (over 5
    -- attempts) remain with attempts >= max for forensic inspection.
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    attempts      INT          NOT NULL DEFAULT 0,
    last_error    TEXT         NULL
);

CREATE INDEX idx_outbox_ready
    ON outbox_events(next_attempt_at)
    WHERE attempts < 5;
