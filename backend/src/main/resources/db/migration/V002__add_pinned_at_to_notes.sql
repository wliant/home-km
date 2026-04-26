ALTER TABLE notes ADD COLUMN pinned_at TIMESTAMPTZ NULL;

CREATE INDEX notes_pinned_at_idx
    ON notes (pinned_at DESC, updated_at DESC)
    WHERE pinned_at IS NOT NULL;
