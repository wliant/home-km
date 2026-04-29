ALTER TABLE notes
    ADD COLUMN is_template BOOLEAN NOT NULL DEFAULT FALSE;

-- Templates aren't part of the active note feed; partial index keeps the
-- existing plan unchanged for non-template lookups.
CREATE INDEX idx_notes_templates ON notes(updated_at DESC) WHERE is_template = TRUE;
