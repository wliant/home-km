package com.homekm.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only aggregate dashboard for the household admin. Pure reporting
 * — no PII beyond per-user totals. Backed by aggregate queries that hit
 * the existing schema indexes; no new tables.
 */
@RestController
@RequestMapping("/api/admin/usage")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsageController {

    private final EntityManager em;

    public AdminUsageController(EntityManager em) {
        this.em = em;
    }

    public record UsageSummary(
            long users,
            long activeUsers,
            long notes,
            long files,
            long folders,
            long tags,
            long reminders,
            long savedSearches,
            long storageBytes,
            List<UserStorage> topStorageUsers,
            List<TagUsage> topTags,
            List<FolderUsage> topFolders
    ) {}

    public record UserStorage(long userId, String displayName, long bytes, long fileCount) {}
    public record TagUsage(long tagId, String name, long usageCount) {}
    public record FolderUsage(long folderId, String name, long itemCount) {}

    @GetMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<UsageSummary> usage() {
        long users = scalar("SELECT COUNT(*) FROM users WHERE is_active = true");
        long activeUsers = scalar(
                "SELECT COUNT(DISTINCT user_id) FROM refresh_tokens "
                        + "WHERE last_seen_at > now() - INTERVAL '30 days' AND revoked_at IS NULL");
        long notes = scalar("SELECT COUNT(*) FROM notes WHERE deleted_at IS NULL AND is_template = false");
        long files = scalar("SELECT COUNT(*) FROM files WHERE deleted_at IS NULL");
        long folders = scalar("SELECT COUNT(*) FROM folders WHERE deleted_at IS NULL");
        long tags = scalar("SELECT COUNT(*) FROM tags");
        long reminders = scalar("SELECT COUNT(*) FROM reminders");
        long savedSearches = scalar("SELECT COUNT(*) FROM saved_searches");
        long storageBytes = scalar("SELECT COALESCE(SUM(size_bytes), 0) FROM files WHERE deleted_at IS NULL");

        List<UserStorage> top = ((List<Object[]>) em.createNativeQuery("""
                SELECT u.id, u.display_name,
                       COALESCE(SUM(f.size_bytes), 0)::bigint AS bytes,
                       COUNT(f.id)::bigint AS file_count
                FROM users u
                LEFT JOIN files f ON f.owner_id = u.id AND f.deleted_at IS NULL
                GROUP BY u.id, u.display_name
                ORDER BY bytes DESC
                LIMIT 10
                """).getResultList()).stream()
                .map(r -> new UserStorage(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        ((Number) r[2]).longValue(),
                        ((Number) r[3]).longValue()))
                .toList();

        List<TagUsage> topTags = ((List<Object[]>) em.createNativeQuery("""
                SELECT t.id, t.name, COUNT(g.id)::bigint AS uses
                FROM tags t
                LEFT JOIN taggings g ON g.tag_id = t.id
                GROUP BY t.id, t.name
                ORDER BY uses DESC
                LIMIT 10
                """).getResultList()).stream()
                .map(r -> new TagUsage(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        ((Number) r[2]).longValue()))
                .toList();

        List<FolderUsage> topFolders = ((List<Object[]>) em.createNativeQuery("""
                SELECT f.id, f.name,
                       (SELECT COUNT(*) FROM notes n WHERE n.folder_id = f.id AND n.deleted_at IS NULL)
                     + (SELECT COUNT(*) FROM files fi WHERE fi.folder_id = f.id AND fi.deleted_at IS NULL) AS items
                FROM folders f
                WHERE f.deleted_at IS NULL
                ORDER BY items DESC
                LIMIT 10
                """).getResultList()).stream()
                .map(r -> new FolderUsage(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        ((Number) r[2]).longValue()))
                .toList();

        return ResponseEntity.ok(new UsageSummary(
                users, activeUsers, notes, files, folders, tags, reminders, savedSearches,
                storageBytes, top, topTags, topFolders));
    }

    private long scalar(String sql) {
        Query q = em.createNativeQuery(sql);
        Object v = q.getSingleResult();
        return v == null ? 0 : ((Number) v).longValue();
    }
}
