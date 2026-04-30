CREATE TABLE data_export_requests (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- PENDING → READY → EXPIRED. Workers move pending → ready; the periodic
    -- cleanup transitions ready → expired.
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    -- MinIO key for the assembled ZIP. Null while pending.
    minio_key    VARCHAR(1000) NULL,
    size_bytes   BIGINT       NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ready_at     TIMESTAMPTZ  NULL,
    -- Hard delete the ZIP after this. Defaults to 24h post-ready.
    expires_at   TIMESTAMPTZ  NULL,
    error_message TEXT        NULL
);

CREATE INDEX idx_export_requests_user
    ON data_export_requests(user_id, created_at DESC);

CREATE INDEX idx_export_requests_pending
    ON data_export_requests(created_at)
    WHERE status = 'PENDING';
