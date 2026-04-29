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
}
