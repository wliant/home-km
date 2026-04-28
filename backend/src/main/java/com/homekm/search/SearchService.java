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

        List<SearchResult> results = new ArrayList<>();

        if (includeNote) results.addAll(searchNotes(q, o, childOnly));
        if (includeFile) results.addAll(searchFiles(q, o, childOnly));
        if (includeFolder) results.addAll(searchFolders(q, o, childOnly));

        results.sort((a, b) -> b.updatedAt().compareTo(a.updatedAt()));

        int from = page * size, to = Math.min(from + size, results.size());
        List<SearchResult> pageContent = from >= results.size() ? List.of() : results.subList(from, to);

        return PageResponse.of(new PageImpl<>(pageContent, PageRequest.of(page, size), results.size()));
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
                row[6] instanceof java.sql.Timestamp ts ? ts.toInstant() : Instant.now()
        )).toList();
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
