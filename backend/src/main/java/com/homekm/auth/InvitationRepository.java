package com.homekm.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    List<Invitation> findAllByOrderByCreatedAtDesc();

    boolean existsByEmailAndAcceptedAtIsNull(String email);
}
