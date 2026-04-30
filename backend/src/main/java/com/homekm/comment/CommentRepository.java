package com.homekm.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.itemType = :itemType AND c.itemId = :itemId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findByItem(@Param("itemType") Comment.ItemType itemType, @Param("itemId") Long itemId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.itemType = :itemType AND c.itemId = :itemId AND c.deletedAt IS NULL")
    long countActiveByItem(@Param("itemType") Comment.ItemType itemType, @Param("itemId") Long itemId);
}
