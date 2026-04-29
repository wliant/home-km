ALTER TABLE notes
    ADD COLUMN child_safe_review_at TIMESTAMPTZ NULL;
ALTER TABLE files
    ADD COLUMN child_safe_review_at TIMESTAMPTZ NULL;

-- Recently-created items the admin hasn't ruled on yet. Partial indexes
-- keep the lookup fast as the corpus grows; reviewed items disappear from
-- the queue and the index doesn't grow with them.
CREATE INDEX idx_notes_review_pending ON notes(created_at DESC)
    WHERE child_safe_review_at IS NULL AND deleted_at IS NULL AND is_template = false;
-- files uses `uploaded_at` rather than `created_at`.
CREATE INDEX idx_files_review_pending ON files(uploaded_at DESC)
    WHERE child_safe_review_at IS NULL AND deleted_at IS NULL;
