package com.homekm.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaggingRepository extends JpaRepository<Tagging, Long> {

    List<Tagging> findByEntityTypeAndEntityId(String entityType, Long entityId);

    Optional<Tagging> findByTagIdAndEntityTypeAndEntityId(Long tagId, String entityType, Long entityId);

    boolean existsByTagIdAndEntityTypeAndEntityId(Long tagId, String entityType, Long entityId);

    long countByEntityTypeAndEntityId(String entityType, Long entityId);

    @Modifying
    @Query("DELETE FROM Tagging t WHERE t.entityType = :type AND t.entityId = :id")
    void deleteByEntityTypeAndEntityId(@Param("type") String type, @Param("id") Long id);

    /**
     * Move every tagging from {@code sourceTagId} to {@code targetTagId},
     * skipping rows the target already covers (the unique index on
     * (tag_id, entity_type, entity_id) would reject a naive UPDATE), then
     * delete the source's leftover taggings.
     */
    @Modifying
    @Query(value = """
        WITH moved AS (
            UPDATE taggings t SET tag_id = :targetTagId
            WHERE t.tag_id = :sourceTagId
              AND NOT EXISTS (
                SELECT 1 FROM taggings t2
                WHERE t2.tag_id = :targetTagId
                  AND t2.entity_type = t.entity_type
                  AND t2.entity_id = t.entity_id
              )
            RETURNING 1
        )
        SELECT COUNT(*) FROM moved
        """, nativeQuery = true)
    int moveTaggings(@Param("sourceTagId") Long sourceTagId,
                     @Param("targetTagId") Long targetTagId);

    @Modifying
    @Query("DELETE FROM Tagging t WHERE t.tag.id = :tagId")
    void deleteByTagId(@Param("tagId") Long tagId);

    /**
     * Tag IDs applied to items sharing {@code folderId} with the given
     * entity (excluding the entity itself). Powers the folder-neighbour
     * signal in the tag-suggestion service. Duplicates are deliberate so
     * the caller can rank by frequency.
     */
    @Query(value = """
        SELECT t.tag_id FROM taggings t
        WHERE NOT (t.entity_type = :selfType AND t.entity_id = :selfId)
          AND (
            (t.entity_type = 'note'   AND t.entity_id IN (SELECT id FROM notes  WHERE folder_id = :folderId AND deleted_at IS NULL))
         OR (t.entity_type = 'file'   AND t.entity_id IN (SELECT id FROM files  WHERE folder_id = :folderId AND deleted_at IS NULL))
         OR (t.entity_type = 'folder' AND t.entity_id IN (SELECT id FROM folders WHERE parent_id = :folderId AND deleted_at IS NULL))
          )
        """, nativeQuery = true)
    List<Long> findFolderNeighbourTagIds(@Param("folderId") Long folderId,
                                          @Param("selfType") String selfType,
                                          @Param("selfId") Long selfId);

    /** Tags that have ever co-appeared on the same entity as {@code tagId}. */
    @Query(value = """
        SELECT b.tag_id FROM taggings a
        JOIN taggings b ON a.entity_type = b.entity_type AND a.entity_id = b.entity_id
        WHERE a.tag_id = :tagId AND b.tag_id <> :tagId
        """, nativeQuery = true)
    List<Long> findCoOccurringTagIds(@Param("tagId") Long tagId);
}
