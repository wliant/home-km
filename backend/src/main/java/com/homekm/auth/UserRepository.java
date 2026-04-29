package com.homekm.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByIcsToken(String icsToken);

    @Query("SELECT COUNT(u) FROM User u WHERE u.admin = true AND u.active = true AND u.id <> :excludeId")
    long countOtherActiveAdmins(@Param("excludeId") Long excludeId);
}
