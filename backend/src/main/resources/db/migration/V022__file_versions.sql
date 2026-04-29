CREATE TABLE file_versions (
    id              BIGSERIAL PRIMARY KEY,
    file_id         BIGINT       NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    minio_key       VARCHAR(1000) NOT NULL,
    filename        VARCHAR(500) NOT NULL,
    mime_type       VARCHAR(127) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    uploaded_by     BIGINT       NOT NULL REFERENCES users(id),
    uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- The current head row is also recorded here so the version list always
    -- shows the latest state alongside the historical ones.
    is_current      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_file_versions_file ON file_versions(file_id, uploaded_at DESC);
CREATE UNIQUE INDEX uq_file_versions_current ON file_versions(file_id) WHERE is_current = TRUE;
