package com.homekm.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileShareLinkRepository extends JpaRepository<FileShareLink, Long> {

    Optional<FileShareLink> findByTokenHash(String tokenHash);

    List<FileShareLink> findByFileIdOrderByCreatedAtDesc(Long fileId);
}
