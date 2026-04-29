ALTER TABLE folders
    ADD COLUMN archived_at TIMESTAMPTZ NULL,
    ADD COLUMN color       VARCHAR(7) NULL,
    ADD COLUMN icon        VARCHAR(32) NULL;

-- Active folder lookups (sidebar tree, list endpoints) all need to skip
-- archived rows; partial index keeps the query plan unchanged.
CREATE INDEX idx_folders_active ON folders(parent_id) WHERE archived_at IS NULL;
