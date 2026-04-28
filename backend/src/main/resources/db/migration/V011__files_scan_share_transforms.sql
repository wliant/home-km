ALTER TABLE files
    ADD COLUMN scan_status   VARCHAR(16) NOT NULL DEFAULT 'CLEAN',
    ADD COLUMN scanned_at    TIMESTAMPTZ NULL;

CREATE TABLE file_share_links (
    id              BIGSERIAL    PRIMARY KEY,
    token_hash      VARCHAR(64)  NOT NULL UNIQUE,
    file_id         BIGINT       NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    password_hash   VARCHAR(120) NULL,
    max_downloads   INT          NULL,
    download_count  INT          NOT NULL DEFAULT 0,
    created_by      BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ  NULL
);
CREATE INDEX idx_file_share_links_file ON file_share_links(file_id);
CREATE INDEX idx_file_share_links_exp  ON file_share_links(expires_at);

CREATE TABLE file_transforms (
    id              BIGSERIAL    PRIMARY KEY,
    file_id         BIGINT       NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    variant         VARCHAR(16)  NOT NULL,
    minio_key       VARCHAR(1000) NOT NULL,
    width           INT          NOT NULL,
    height          INT          NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    mime_type       VARCHAR(80)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (file_id, variant)
);
CREATE INDEX idx_file_transforms_file ON file_transforms(file_id);
