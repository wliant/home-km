package com.homekm.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MentionInboxRepository extends JpaRepository<MentionInbox, Long> {

    @Query("SELECT m FROM MentionInbox m WHERE m.userId = :userId ORDER BY m.createdAt DESC")
    List<MentionInbox> findByUserIdNewestFirst(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM MentionInbox m WHERE m.userId = :userId AND m.readAt IS NULL")
    long countUnreadByUserId(@Param("userId") Long userId);

    Optional<MentionInbox> findByIdAndUserId(Long id, Long userId);
}
