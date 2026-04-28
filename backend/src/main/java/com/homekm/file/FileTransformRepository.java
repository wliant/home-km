package com.homekm.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileTransformRepository extends JpaRepository<FileTransform, Long> {

    List<FileTransform> findByFileId(Long fileId);

    Optional<FileTransform> findByFileIdAndVariant(Long fileId, String variant);
}
