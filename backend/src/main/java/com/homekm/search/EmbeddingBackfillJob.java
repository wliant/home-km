package com.homekm.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Periodically embeds rows whose {@code embedding IS NULL}. Bounded by
 * {@code app.embedding.backfill-batch} per tick (default 25) so a household
 * Ollama instance isn't overwhelmed when embeddings are first turned on or
 * when the model changes (operators truncate the column to force a re-index).
 *
 * The job is a no-op when embeddings are disabled — costs nothing.
 */
@Component
public class EmbeddingBackfillJob {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingBackfillJob.class);

    @PersistenceContext
    private EntityManager em;

    private final EmbeddingService embeddings;
    private final EmbeddingIndexer indexer;

    @Value("${app.embedding.backfill-batch:25}")
    private int batchSize;

    public EmbeddingBackfillJob(EmbeddingService embeddings, EmbeddingIndexer indexer) {
        this.embeddings = embeddings;
        this.indexer = indexer;
    }

    /**
     * Runs every minute when enabled. Pulling and writing in separate
     * transactions keeps long Ollama calls out of the read transaction.
     */
    @Scheduled(fixedDelayString = "${app.embedding.backfill-interval-ms:60000}")
    public void tick() {
        if (!embeddings.isEnabled()) return;
        int wrote = 0;
        wrote += backfillNotes();
        wrote += backfillFiles();
        wrote += backfillFolders();
        if (wrote > 0) log.info("embedding backfill: wrote {} embeddings this tick", wrote);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    int backfillNotes() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id, title, body FROM notes WHERE embedding IS NULL AND deleted_at IS NULL ORDER BY id LIMIT :n")
                .setParameter("n", batchSize)
                .getResultList();
        for (Object[] r : rows) {
            indexer.indexNote(((Number) r[0]).longValue(), (String) r[1], (String) r[2]);
        }
        return rows.size();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    int backfillFiles() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id, filename, description FROM files WHERE embedding IS NULL AND deleted_at IS NULL ORDER BY id LIMIT :n")
                .setParameter("n", batchSize)
                .getResultList();
        for (Object[] r : rows) {
            indexer.indexFile(((Number) r[0]).longValue(), (String) r[1], (String) r[2]);
        }
        return rows.size();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    int backfillFolders() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id, name, description FROM folders WHERE embedding IS NULL AND deleted_at IS NULL ORDER BY id LIMIT :n")
                .setParameter("n", batchSize)
                .getResultList();
        for (Object[] r : rows) {
            indexer.indexFolder(((Number) r[0]).longValue(), (String) r[1], (String) r[2]);
        }
        return rows.size();
    }
}
