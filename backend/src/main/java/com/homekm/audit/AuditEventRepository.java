package com.homekm.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);

    @Query("SELECT e FROM AuditEvent e WHERE " +
           "(:actorId IS NULL OR e.actorUserId = :actorId) AND " +
           "(:action IS NULL OR e.action = :action) AND " +
           "(:from IS NULL OR e.occurredAt >= :from) AND " +
           "(:to IS NULL OR e.occurredAt <= :to) " +
           "ORDER BY e.occurredAt DESC")
    Page<AuditEvent> findFiltered(@Param("actorId") Long actorId,
                                   @Param("action") String action,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to,
                                   Pageable pageable);
}
