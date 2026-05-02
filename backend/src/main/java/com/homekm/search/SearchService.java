package com.homekm.search;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.PageResponse;
import com.homekm.search.dto.SearchResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final EntityManager em;
    private final EmbeddingService embeddings;

    public SearchService(EntityManager em, EmbeddingService embeddings) {
        this.em = em;
        this.embeddings = embeddings;
    }

    public PageResponse<SearchResult> search(String q, SearchOpts o, int page, int size, UserPrincipal principal) {
        boolean childOnly = principal.isChild();
        boolean includeNote = o.types() == null || o.types().isEmpty() || o.types().contains("note");
        boolean includeFile = o.types() == null || o.types().isEmpty() || o.types().contains("file");
        boolean includeFolder = o.types() == null || o.types().isEmpty() || o.types().contains("folder");

        // Smart mode pivots from FTS to pgvector cosine ranking. Falls back to
        // FTS silently when the embedding service is disabled or the query
        // can't be embedded — callers can detect this via the result count.
        String queryVec = null;
        if (o.smart() && embeddings.isEnabled()) {
            float[] vec = embeddings.embed(q);
            if (vec != null && vec.length > 0) queryVec = toVectorLiteral(vec);
        }

        List<SearchResult> results = new ArrayList<>();
        if (queryVec != null) {
            int k = Math.max(50, (page + 1) * size);
            if (includeNote) results.addAll(semanticSearchNotes(queryVec, o, childOnly, k));
            if (includeFile) results.addAll(semanticSearchFiles(queryVec, o, childOnly, k));
            if (includeFolder) results.addAll(semanticSearchFolders(queryVec, childOnly, k));
            // Vector results already arrive ordered by ascending cosine
            // distance per type; merge by their natural order — most-similar
            // first across types.
            results.sort((a, b) -> Double.compare(b.score(), a.score()));
        } else {
            if (includeNote) results.addAll(searchNotes(q, o, childOnly));
            if (includeFile) results.addAll(searchFiles(q, o, childOnly));
            if (includeFolder) results.addAll(searchFolders(q, o, childOnly));
            results.sort((a, b) -> b.updatedAt().compareTo(a.updatedAt()));
        }

        int from = page * size, to = Math.min(from + size, results.size());
        List<SearchResult> pageContent = from >= results.size() ? List.of() : results.subList(from, to);

        return PageResponse.of(new PageImpl<>(pageContent, PageRequest.of(page, size), results.size()));
    }

    /**
     * pg_trgm similarity-based suggestion for typo-tolerant fallback. Queries
     * the indexed {@code tags.name} trigram column plus full-text columns;
     * returns the highest-similarity term only if it crosses a 0.3 threshold.
     * Returns {@code null} when nothing is close enough — caller should not
     * render a "did you mean" hint in that case.
     */
    @SuppressWarnings("unchecked")
    public String findSuggestion(String q) {
        if (q == null || q.isBlank() || q.length() < 3) return null;
        // UNION across small candidate pools. tags.name has a trigram GIN
        // index (V001); notes.title / folders.name / files.filename are
        // sequential scans, but the row counts in a household app are small
        // enough that this is fast in practice (<1ms for ~1k rows).
        String sql = """
            SELECT term, sim FROM (
              SELECT name AS term, similarity(name, :q) AS sim FROM tags
              UNION ALL
              SELECT title, similarity(title, :q) FROM notes WHERE deleted_at IS NULL AND title IS NOT NULL
              UNION ALL
              SELECT name, similarity(name, :q) FROM folders WHERE name IS NOT NULL
              UNION ALL
              SELECT filename, similarity(filename, :q) FROM files WHERE filename IS NOT NULL
            ) candidates
            WHERE sim > 0.3 AND lower(term) <> lower(:q)
            ORDER BY sim DESC
            LIMIT 1
            """;
        Query query = em.createNativeQuery(sql);
        query.setParameter("q", q);
        List<Object[]> rows = (List<Object[]>) query.getResultList();
        if (rows.isEmpty()) return null;
        Object term = rows.get(0)[0];
        return term != null ? term.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> searchNotes(String q, SearchOpts o, boolean childOnly) {
        StringBuilder sql = new StringBuilder("""
            SELECT n.id, 'note' as type, n.title,
                   ts_headline('english', COALESCE(n.body,''), plainto_tsquery('english', :q),
                       'MaxFragments=1,MaxWords=15,MinWords=5') as excerpt,
                   n.folder_id, n.is_child_safe, n.updated_at
            FROM notes n
            WHERE n.search_vector @@ plainto_tsquery('english', :q)
            AND n.deleted_at IS NULL
            """);
        if (childOnly) sql.append(" AND n.is_child_safe = true");
        if (o.childSafe() != null) sql.append(" AND n.is_child_safe = ").append(o.childSafe());
        if (o.ownerId() != null) sql.append(" AND n.owner_id = :ownerId");
        if (o.from() != null) sql.append(" AND n.updated_at >= :fromTs");
        if (o.to() != null) sql.append(" AND n.updated_at <= :toTs");
        if (o.hasReminder() != null) {
            sql.append(o.hasReminder()
                    ? " AND EXISTS (SELECT 1 FROM reminders r WHERE r.note_id = n.id)"
                    : " AND NOT EXISTS (SELECT 1 FROM reminders r WHERE r.note_id = n.id)");
        }
        if (o.folderId() != null) {
            if (o.includeSubfolders()) sql.append(" AND n.folder_id IN (").append(folderSubtreeSQL()).append(")");
            else sql.append(" AND n.folder_id = :folderId");
        }
        if (o.tagIds() != null && !o.tagIds().isEmpty()) {
            sql.append(" AND (SELECT COUNT(DISTINCT t.tag_id) FROM taggings t WHERE t.entity_type='note' AND t.entity_id=n.id AND t.tag_id IN (")
               .append(joinLongs(o.tagIds())).append(")) = ").append(o.tagIds().size());
        }

        Query query = em.createNativeQuery(sql.toString());
        bindCommonParams(query, q, o);
        return mapRows((List<Object[]>) query.getResultList(), "note");
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> searchFiles(String q, SearchOpts o, boolean childOnly) {
        StringBuilder sql = new StringBuilder("""
            SELECT f.id, 'file' as type, f.filename,
                   ts_headline('english', COALESCE(f.description,''), plainto_tsquery('english', :q),
                       'MaxFragments=1,MaxWords=15,MinWords=5') as excerpt,
                   f.folder_id, f.is_child_safe, f.updated_at
            FROM files f
            WHERE f.search_vector @@ plainto_tsquery('english', :q)
            AND f.deleted_at IS NULL
            """);
        if (childOnly) sql.append(" AND f.is_child_safe = true");
        if (o.childSafe() != null) sql.append(" AND f.is_child_safe = ").append(o.childSafe());
        if (o.ownerId() != null) sql.append(" AND f.owner_id = :ownerId");
        if (o.from() != null) sql.append(" AND f.updated_at >= :fromTs");
        if (o.to() != null) sql.append(" AND f.updated_at <= :toTs");
        if (o.mimePrefix() != null && !o.mimePrefix().isBlank()) {
            sql.append(" AND f.mime_type LIKE :mimePrefix");
        }
        if (o.folderId() != null) {
            if (o.includeSubfolders()) sql.append(" AND f.folder_id IN (").append(folderSubtreeSQL()).append(")");
            else sql.append(" AND f.folder_id = :folderId");
        }
        if (o.tagIds() != null && !o.tagIds().isEmpty()) {
            sql.append(" AND (SELECT COUNT(DISTINCT t.tag_id) FROM taggings t WHERE t.entity_type='file' AND t.entity_id=f.id AND t.tag_id IN (")
               .append(joinLongs(o.tagIds())).append(")) = ").append(o.tagIds().size());
        }

        Query query = em.createNativeQuery(sql.toString());
        bindCommonParams(query, q, o);
        if (o.mimePrefix() != null && !o.mimePrefix().isBlank()) {
            query.setParameter("mimePrefix", o.mimePrefix() + "%");
        }
        return mapRows((List<Object[]>) query.getResultList(), "file");
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> searchFolders(String q, SearchOpts o, boolean childOnly) {
        StringBuilder sql = new StringBuilder("""
            SELECT f.id, 'folder' as type, f.name,
                   ts_headline('english', COALESCE(f.description,''), plainto_tsquery('english', :q),
                       'MaxFragments=1,MaxWords=15,MinWords=5') as excerpt,
                   f.parent_id as folder_id, f.is_child_safe, f.updated_at
            FROM folders f
            WHERE f.search_vector @@ plainto_tsquery('english', :q)
            AND f.deleted_at IS NULL
            """);
        if (childOnly) sql.append(" AND f.is_child_safe = true");
        if (o.tagIds() != null && !o.tagIds().isEmpty()) {
            sql.append(" AND (SELECT COUNT(DISTINCT t.tag_id) FROM taggings t WHERE t.entity_type='folder' AND t.entity_id=f.id AND t.tag_id IN (")
               .append(joinLongs(o.tagIds())).append(")) = ").append(o.tagIds().size());
        }

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("q", q);
        return mapRows((List<Object[]>) query.getResultList(), "folder");
    }

    private void bindCommonParams(Query query, String q, SearchOpts o) {
        query.setParameter("q", q);
        if (o.folderId() != null) query.setParameter("folderId", o.folderId());
        if (o.ownerId() != null) query.setParameter("ownerId", o.ownerId());
        if (o.from() != null) query.setParameter("fromTs", java.sql.Timestamp.from(o.from()));
        if (o.to() != null) query.setParameter("toTs", java.sql.Timestamp.from(o.to()));
    }

    private String folderSubtreeSQL() {
        return """
            WITH RECURSIVE subtree AS (
                SELECT id FROM folders WHERE id = :folderId
                UNION ALL SELECT f.id FROM folders f JOIN subtree s ON f.parent_id = s.id
            ) SELECT id FROM subtree
            """;
    }

    private String joinLongs(List<Long> ids) {
        return ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("0");
    }

    private List<SearchResult> mapRows(List<Object[]> rows, String type) {
        return rows.stream().map(row -> new SearchResult(
                ((Number) row[0]).longValue(),
                type,
                (String) row[2],
                (String) row[3],
                row[4] != null ? ((Number) row[4]).longValue() : null,
                Boolean.TRUE.equals(row[5]),
                row[6] instanceof java.sql.Timestamp ts ? ts.toInstant() : Instant.now(),
                0.0
        )).toList();
    }

    /**
     * pgvector accepts the literal {@code '[0.1,0.2,...]'} via an explicit
     * cast in SQL; serializing here keeps the SearchService self-contained
     * and avoids a Hibernate UserType for what is otherwise a one-shot bind.
     */
    private static String toVectorLiteral(float[] vec) {
        StringBuilder sb = new StringBuilder(vec.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> semanticSearchNotes(String vec, SearchOpts o, boolean childOnly, int k) {
        StringBuilder sql = new StringBuilder("""
            SELECT n.id, 'note' as type, n.title,
                   COALESCE(left(n.body, 200), '') as excerpt,
                   n.folder_id, n.is_child_safe, n.updated_at,
                   (n.embedding <=> CAST(:vec AS vector)) as distance
            FROM notes n
            WHERE n.embedding IS NOT NULL
            AND n.deleted_at IS NULL
            """);
        if (childOnly) sql.append(" AND n.is_child_safe = true");
        if (o.childSafe() != null) sql.append(" AND n.is_child_safe = ").append(o.childSafe());
        if (o.ownerId() != null) sql.append(" AND n.owner_id = :ownerId");
        if (o.from() != null) sql.append(" AND n.updated_at >= :fromTs");
        if (o.to() != null) sql.append(" AND n.updated_at <= :toTs");
        if (o.hasReminder() != null) {
            sql.append(o.hasReminder()
                    ? " AND EXISTS (SELECT 1 FROM reminders r WHERE r.note_id = n.id)"
                    : " AND NOT EXISTS (SELECT 1 FROM reminders r WHERE r.note_id = n.id)");
        }
        if (o.folderId() != null) {
            if (o.includeSubfolders()) sql.append(" AND n.folder_id IN (").append(folderSubtreeSQL()).append(")");
            else sql.append(" AND n.folder_id = :folderId");
        }
        if (o.tagIds() != null && !o.tagIds().isEmpty()) {
            sql.append(" AND (SELECT COUNT(DISTINCT t.tag_id) FROM taggings t WHERE t.entity_type='note' AND t.entity_id=n.id AND t.tag_id IN (")
               .append(joinLongs(o.tagIds())).append(")) = ").append(o.tagIds().size());
        }
        sql.append(" ORDER BY distance ASC LIMIT ").append(k);

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("vec", vec);
        if (o.folderId() != null) query.setParameter("folderId", o.folderId());
        if (o.ownerId() != null) query.setParameter("ownerId", o.ownerId());
        if (o.from() != null) query.setParameter("fromTs", java.sql.Timestamp.from(o.from()));
        if (o.to() != null) query.setParameter("toTs", java.sql.Timestamp.from(o.to()));
        return mapVectorRows((List<Object[]>) query.getResultList(), "note");
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> semanticSearchFiles(String vec, SearchOpts o, boolean childOnly, int k) {
        StringBuilder sql = new StringBuilder("""
            SELECT f.id, 'file' as type, f.filename,
                   COALESCE(left(f.description, 200), '') as excerpt,
                   f.folder_id, f.is_child_safe, f.updated_at,
                   (f.embedding <=> CAST(:vec AS vector)) as distance
            FROM files f
            WHERE f.embedding IS NOT NULL
            AND f.deleted_at IS NULL
            """);
        if (childOnly) sql.append(" AND f.is_child_safe = true");
        if (o.childSafe() != null) sql.append(" AND f.is_child_safe = ").append(o.childSafe());
        if (o.ownerId() != null) sql.append(" AND f.owner_id = :ownerId");
        if (o.from() != null) sql.append(" AND f.updated_at >= :fromTs");
        if (o.to() != null) sql.append(" AND f.updated_at <= :toTs");
        if (o.mimePrefix() != null && !o.mimePrefix().isBlank()) {
            sql.append(" AND f.mime_type LIKE :mimePrefix");
        }
        if (o.folderId() != null) {
            if (o.includeSubfolders()) sql.append(" AND f.folder_id IN (").append(folderSubtreeSQL()).append(")");
            else sql.append(" AND f.folder_id = :folderId");
        }
        if (o.tagIds() != null && !o.tagIds().isEmpty()) {
            sql.append(" AND (SELECT COUNT(DISTINCT t.tag_id) FROM taggings t WHERE t.entity_type='file' AND t.entity_id=f.id AND t.tag_id IN (")
               .append(joinLongs(o.tagIds())).append(")) = ").append(o.tagIds().size());
        }
        sql.append(" ORDER BY distance ASC LIMIT ").append(k);

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("vec", vec);
        if (o.folderId() != null) query.setParameter("folderId", o.folderId());
        if (o.ownerId() != null) query.setParameter("ownerId", o.ownerId());
        if (o.from() != null) query.setParameter("fromTs", java.sql.Timestamp.from(o.from()));
        if (o.to() != null) query.setParameter("toTs", java.sql.Timestamp.from(o.to()));
        if (o.mimePrefix() != null && !o.mimePrefix().isBlank()) {
            query.setParameter("mimePrefix", o.mimePrefix() + "%");
        }
        return mapVectorRows((List<Object[]>) query.getResultList(), "file");
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> semanticSearchFolders(String vec, boolean childOnly, int k) {
        StringBuilder sql = new StringBuilder("""
            SELECT f.id, 'folder' as type, f.name,
                   COALESCE(left(f.description, 200), '') as excerpt,
                   f.parent_id as folder_id, f.is_child_safe, f.updated_at,
                   (f.embedding <=> CAST(:vec AS vector)) as distance
            FROM folders f
            WHERE f.embedding IS NOT NULL
            AND f.deleted_at IS NULL
            """);
        if (childOnly) sql.append(" AND f.is_child_safe = true");
        sql.append(" ORDER BY distance ASC LIMIT ").append(k);

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("vec", vec);
        return mapVectorRows((List<Object[]>) query.getResultList(), "folder");
    }

    /**
     * Maps semantic-search rows. Score is {@code 1 - cosine_distance}, which
     * lives in [0, 2] (pgvector cosine distance can exceed 1 for opposing
     * vectors). Clamping isn't necessary for ordering, just for the score
     * field that the UI may show.
     */
    private List<SearchResult> mapVectorRows(List<Object[]> rows, String type) {
        return rows.stream().map(row -> {
            double dist = row[7] != null ? ((Number) row[7]).doubleValue() : 1.0;
            double score = Math.max(0.0, Math.min(1.0, 1.0 - dist));
            return new SearchResult(
                    ((Number) row[0]).longValue(),
                    type,
                    (String) row[2],
                    (String) row[3],
                    row[4] != null ? ((Number) row[4]).longValue() : null,
                    Boolean.TRUE.equals(row[5]),
                    row[6] instanceof java.sql.Timestamp ts ? ts.toInstant() : Instant.now(),
                    score);
        }).toList();
    }

    public record SearchOpts(
            List<String> types,
            Long folderId,
            boolean includeSubfolders,
            List<Long> tagIds,
            Long ownerId,
            String mimePrefix,
            Boolean hasReminder,
            Boolean childSafe,
            Instant from,
            Instant to,
            boolean smart
    ) {}
}
