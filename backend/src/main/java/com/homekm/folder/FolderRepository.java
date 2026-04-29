package com.homekm.folder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    boolean existsByParentIdAndNameIgnoreCase(Long parentId, String name);

    boolean existsByParentIsNullAndNameIgnoreCase(String name);

    boolean existsByParentId(Long parentId);

    @Query("SELECT f FROM Folder f WHERE f.parent IS NULL AND f.deletedAt IS NULL")
    List<Folder> findByParentIsNull();

    @Query("SELECT f FROM Folder f WHERE f.parent.id = :parentId AND f.deletedAt IS NULL")
    List<Folder> findByParentId(@Param("parentId") Long parentId);

    @Query(value = """
        WITH RECURSIVE ancestors AS (
            SELECT id, parent_id FROM folders WHERE id = :potentialParentId
            UNION ALL
            SELECT f.id, f.parent_id FROM folders f
            JOIN ancestors a ON f.id = a.parent_id
        )
        SELECT COUNT(*) > 0 FROM ancestors WHERE id = :folderId
        """, nativeQuery = true)
    boolean wouldCreateCycle(@Param("folderId") Long folderId,
                             @Param("potentialParentId") Long potentialParentId);

    @Query(value = """
        WITH RECURSIVE depth_count AS (
            SELECT id, parent_id, 1 AS depth FROM folders WHERE id = :parentId
            UNION ALL
            SELECT f.id, f.parent_id, dc.depth + 1 FROM folders f
            JOIN depth_count dc ON f.id = dc.parent_id
        )
        SELECT COALESCE(MAX(depth), 0) FROM depth_count
        """, nativeQuery = true)
    int countAncestorDepth(@Param("parentId") Long parentId);

    @Query(value = """
        WITH RECURSIVE subtree AS (
            SELECT id FROM folders WHERE id = :folderId
            UNION ALL
            SELECT f.id FROM folders f JOIN subtree s ON f.parent_id = s.id
        )
        SELECT id FROM subtree WHERE id != :folderId
        """, nativeQuery = true)
    List<Long> findDescendantIds(@Param("folderId") Long folderId);

    @Modifying
    @Query("UPDATE Folder f SET f.childSafe = true WHERE f.id IN :ids")
    void markChildSafeByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE Folder f SET f.childSafe = :safe WHERE f.id = :id")
    void updateChildSafe(@Param("id") Long id, @Param("safe") boolean safe);

    Optional<Folder> findByIdAndChildSafe(Long id, boolean childSafe);

    @Query("SELECT f FROM Folder f WHERE f.id = :id AND f.childSafe = :childSafe AND f.deletedAt IS NULL")
    Optional<Folder> findByIdAndChildSafeAndDeletedAtIsNull(@Param("id") Long id, @Param("childSafe") boolean childSafe);

    @Query("SELECT f FROM Folder f WHERE f.deletedAt IS NULL AND f.archivedAt IS NULL")
    List<Folder> findAllActive();

    @Query("SELECT f FROM Folder f WHERE f.deletedAt IS NULL AND f.archivedAt IS NOT NULL ORDER BY f.archivedAt DESC")
    List<Folder> findAllArchived();

    @Query("SELECT f FROM Folder f WHERE f.deletedAt IS NOT NULL ORDER BY f.deletedAt DESC")
    List<Folder> findAllDeleted();

    @Query("SELECT f FROM Folder f WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<Folder> findActiveById(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Folder f WHERE f.deletedAt IS NOT NULL AND f.deletedAt < :cutoff")
    int purgeDeletedBefore(@Param("cutoff") Instant cutoff);
}
