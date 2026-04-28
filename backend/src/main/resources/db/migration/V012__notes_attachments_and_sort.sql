CREATE TABLE note_attachments (
    id          BIGSERIAL    PRIMARY KEY,
    note_id     BIGINT       NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    file_id     BIGINT       NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    position    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (note_id, file_id)
);
CREATE INDEX idx_note_attachments_note ON note_attachments(note_id);

ALTER TABLE folders ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
CREATE INDEX idx_folders_parent_sort ON folders(parent_id, sort_order);
