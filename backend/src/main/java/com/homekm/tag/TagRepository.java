package com.homekm.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Tag> findByNameIgnoreCase(String name);

    @Query(value = "SELECT * FROM tags WHERE name ILIKE '%' || :q || '%' ORDER BY name LIMIT 10",
           nativeQuery = true)
    List<Tag> findByNameContaining(@Param("q") String q);
}
