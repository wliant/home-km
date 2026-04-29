CREATE TABLE note_revisions (
    id          BIGSERIAL PRIMARY KEY,
    note_id     BIGINT       NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    title       VARCHAR(500) NOT NULL,
    body        TEXT,
    label       VARCHAR(50)  NOT NULL,
    edited_by   BIGINT       NOT NULL REFERENCES users(id),
    edited_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Look up "last 50 revisions for this note ordered newest-first" — covers
-- both the History tab and the per-note retention cap query.
CREATE INDEX idx_note_revisions_note ON note_revisions(note_id, edited_at DESC);
