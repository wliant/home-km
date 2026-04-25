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
}
