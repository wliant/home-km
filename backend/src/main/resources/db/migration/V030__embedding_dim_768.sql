-- Switch the embedding columns from the placeholder vector(1536) reservation
-- in V001/V013 to vector(768) — the dimension produced by the default
-- nomic-embed-text model that the Ollama-backed EmbeddingService ships with.
--
-- Operators using a different model (e.g. text-embedding-3-large at 1536) can
-- write a follow-up migration; the v1 default is local + 768 because that is
-- what the bundled docker compose profile spins up.
--
-- Drop the HNSW indices first so the column type change is allowed; recreate
-- them after the alteration. Any prior embeddings are discarded by USING NULL
-- since dim mismatch can't be cast and the backfill job will refill them.
DROP INDEX IF EXISTS idx_notes_embedding;
DROP INDEX IF EXISTS idx_files_embedding;
DROP INDEX IF EXISTS idx_folders_embedding;

ALTER TABLE notes   ALTER COLUMN embedding TYPE vector(768) USING NULL;
ALTER TABLE files   ALTER COLUMN embedding TYPE vector(768) USING NULL;
ALTER TABLE folders ALTER COLUMN embedding TYPE vector(768) USING NULL;

CREATE INDEX IF NOT EXISTS idx_notes_embedding   ON notes   USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_files_embedding   ON files   USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_folders_embedding ON folders USING hnsw (embedding vector_cosine_ops);
