package com.homekm.search;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    List<SavedSearch> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SavedSearch> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
