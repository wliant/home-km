-- Embeddings for hybrid (lexical + semantic) search.
-- notes.embedding and files.embedding already exist (vector(1536) from V001); add the same to folders
-- and create HNSW indices on all three so cosine queries scale.
ALTER TABLE folders ADD COLUMN embedding vector(1536) NULL;

CREATE INDEX IF NOT EXISTS idx_notes_embedding   ON notes   USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_files_embedding   ON files   USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_folders_embedding ON folders USING hnsw (embedding vector_cosine_ops);
