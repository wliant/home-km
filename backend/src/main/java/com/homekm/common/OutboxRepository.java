package com.homekm.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.nextAttemptAt <= :now AND e.attempts < :maxAttempts
        ORDER BY e.nextAttemptAt ASC
        """)
    List<OutboxEvent> findReady(@Param("now") Instant now,
                                 @Param("maxAttempts") int maxAttempts);
}
