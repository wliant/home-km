package com.homekm.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, Long> {

    @Query("SELECT c FROM MfaRecoveryCode c WHERE c.userId = :userId AND c.usedAt IS NULL")
    List<MfaRecoveryCode> findUnusedByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM MfaRecoveryCode c WHERE c.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
