package com.homekm.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes pgvector embeddings into the entity tables. Used by both inline
 * post-save hooks (best-effort) and the periodic backfill job. Kept separate
 * from {@link SearchService} so call sites don't bring search query state
 * along just to write.
 *
 * Persisted asynchronously: the API path returns to the user as soon as the
 * row is committed; an embedding miss only degrades smart-search recall, not
 * correctness, so it's never worth blocking a save on.
 */
@Component
public class EmbeddingIndexer {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexer.class);

    @PersistenceContext
    private EntityManager em;

    private final EmbeddingService embeddings;

    public EmbeddingIndexer(EmbeddingService embeddings) {
        this.embeddings = embeddings;
    }

    @Async
    @Transactional
    public void indexNote(long noteId, String title, String body) {
        if (!embeddings.isEnabled()) return;
        String text = combine(title, body);
        if (text.isBlank()) return;
        float[] vec = embeddings.embed(text);
        if (vec == null || vec.length == 0) return;
        try {
            em.createNativeQuery("UPDATE notes SET embedding = CAST(:vec AS vector) WHERE id = :id")
                    .setParameter("vec", toVectorLiteral(vec))
                    .setParameter("id", noteId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("note embed write failed id={}: {}", noteId, e.getMessage());
        }
    }

    @Async
    @Transactional
    public void indexFile(long fileId, String filename, String description) {
        if (!embeddings.isEnabled()) return;
        String text = combine(filename, description);
        if (text.isBlank()) return;
        float[] vec = embeddings.embed(text);
        if (vec == null || vec.length == 0) return;
        try {
            em.createNativeQuery("UPDATE files SET embedding = CAST(:vec AS vector) WHERE id = :id")
                    .setParameter("vec", toVectorLiteral(vec))
                    .setParameter("id", fileId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("file embed write failed id={}: {}", fileId, e.getMessage());
        }
    }

    @Async
    @Transactional
    public void indexFolder(long folderId, String name, String description) {
        if (!embeddings.isEnabled()) return;
        String text = combine(name, description);
        if (text.isBlank()) return;
        float[] vec = embeddings.embed(text);
        if (vec == null || vec.length == 0) return;
        try {
            em.createNativeQuery("UPDATE folders SET embedding = CAST(:vec AS vector) WHERE id = :id")
                    .setParameter("vec", toVectorLiteral(vec))
                    .setParameter("id", folderId)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("folder embed write failed id={}: {}", folderId, e.getMessage());
        }
    }

    private static String combine(String a, String b) {
        String aa = a == null ? "" : a;
        String bb = b == null ? "" : b;
        return (aa + " " + bb).trim();
    }

    static String toVectorLiteral(float[] vec) {
        StringBuilder sb = new StringBuilder(vec.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
