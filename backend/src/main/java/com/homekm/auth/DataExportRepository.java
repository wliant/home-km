package com.homekm.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DataExportRepository extends JpaRepository<DataExportRequest, Long> {

    Optional<DataExportRequest> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r FROM DataExportRequest r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<DataExportRequest> findByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM DataExportRequest r WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    List<DataExportRequest> findPending();

    @Query("SELECT r FROM DataExportRequest r WHERE r.status = 'READY' AND r.expiresAt < :now")
    List<DataExportRequest> findExpired(@Param("now") Instant now);
}
