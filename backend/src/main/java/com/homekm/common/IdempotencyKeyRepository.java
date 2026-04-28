package com.homekm.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    @Query("""
        select k from IdempotencyKey k
        where k.keyHash = :keyHash
          and (:userId is null or k.userId = :userId)
          and k.method = :method and k.path = :path
        """)
    Optional<IdempotencyKey> findExisting(@Param("keyHash") String keyHash,
                                          @Param("userId") Long userId,
                                          @Param("method") String method,
                                          @Param("path") String path);

    @Modifying
    @Query("delete from IdempotencyKey k where k.expiresAt < :cutoff")
    int purgeExpired(@Param("cutoff") Instant cutoff);
}
