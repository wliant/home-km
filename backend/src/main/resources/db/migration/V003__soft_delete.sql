ALTER TABLE notes   ADD COLUMN deleted_at TIMESTAMPTZ NULL;
ALTER TABLE files   ADD COLUMN deleted_at TIMESTAMPTZ NULL;
ALTER TABLE folders ADD COLUMN deleted_at TIMESTAMPTZ NULL;

CREATE INDEX idx_notes_not_deleted   ON notes(id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_files_not_deleted   ON files(id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_folders_not_deleted ON folders(id) WHERE deleted_at IS NULL;
