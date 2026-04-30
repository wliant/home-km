package com.homekm.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    Optional<UserGroup> findByKind(UserGroup.Kind kind);
    boolean existsByName(String name);
}
