package com.homekm.search;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.PageResponse;
import com.homekm.search.dto.SearchResult;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final EntityManager em;

    public SearchService(EntityManager em) {
        this.em = em;
    }

    public PageResponse<SearchResult> search(String q, List<String> types, Long folderId,
                                              List<Long> tagIds, int page, int size,
                                              UserPrincipal principal) {
        boolean childOnly = principal.isChild();
        boolean includeNote = types == null || types.isEmpty() || types.contains("note");
        boolean includeFile = types == null || types.isEmpty() || types.contains("file");
        boolean includeFolder = types == null || types.isEmpty() || types.contains("folder");

        List<SearchResult> results = new ArrayList<>();

        if (includeNote) results.addAll(searchNotes(q, folderId, tagIds, childOnly));
        if (includeFile) results.addAll(searchFiles(q, folderId, tagIds, childOnly));
        if (includeFolder) results.addAll(searchFolders(q, tagIds, childOnly));

        results.sort((a, b) -> b.updatedAt().compareTo(a.updatedAt()));

        int from = page * size, to = Math.min(from + size, results.size());
        List<SearchResult> pageContent = from >= results.size() ? List.of() : results.subList(from, to);

        return PageResponse.of(new PageImpl<>(pageContent, PageRequest.of(page, size), results.size()));
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> searchNotes(String q, Long folderId, List<Long> tagIds, boolean childOnly) {
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
        if (folderId != null) sql.append(" AND n.folder_id IN (").append(folderSubtreeSQL()).append(")");
        if (tagIds != null && !tagIds.isEmpty()) {
            // tagIds are typed Long — numeric-only, safe to inline
            sql.append(" AND (SELECT COUNT(DISTINCT t.tag_id) FROM taggings t WHERE t.entity_type='note' AND t.entity_id=n.id AND t.tag_id IN (")
               .append(joinLongs(tagIds)).append(")) = ").append(tagIds.size());
        }

        var query = em.createNativeQuery(sql.toString());
        query.setParameter("q", q);
        if (folderId != null) query.setParameter("folderId", folderId);
        return mapRows((List<Object[]>) query.getResultList(), "note");
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> searchFiles(String q, Long folderId, List<Long> tagIds, boolean childOnly) {
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
        if (folderId != null) sql.append(" AND f.folder_id IN (").append(folderSubtreeSQL()).append(")");
        if (tagIds != null && !tagIds.isEmpty()) {
            sql.append(" AND (SELECT COUNT(DISTINCT t.tag_id) FROM taggings t WHERE t.entity_type='file' AND t.entity_id=f.id AND t.tag_id IN (")
               .append(joinLongs(tagIds)).append(")) = ").append(tagIds.size());
        }

        var query = em.createNativeQuery(sql.toString());
        query.setParameter("q", q);
        if (folderId != null) query.setParameter("folderId", folderId);
        return mapRows((List<Object[]>) query.getResultList(), "file");
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> searchFolders(String q, List<Long> tagIds, boolean childOnly) {
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
        if (tagIds != null && !tagIds.isEmpty()) {
            sql.append(" AND (SELECT COUNT(DISTINCT t.tag_id) FROM taggings t WHERE t.entity_type='folder' AND t.entity_id=f.id AND t.tag_id IN (")
               .append(joinLongs(tagIds)).append(")) = ").append(tagIds.size());
        }

        var query = em.createNativeQuery(sql.toString());
        query.setParameter("q", q);
        return mapRows((List<Object[]>) query.getResultList(), "folder");
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
}
