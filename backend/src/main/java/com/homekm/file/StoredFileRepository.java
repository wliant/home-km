package com.homekm.file;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    Page<StoredFile> findByFolderIdOrderByUploadedAtDesc(Long folderId, Pageable pageable);

    Page<StoredFile> findByFolderIsNullOrderByUploadedAtDesc(Pageable pageable);

    Page<StoredFile> findByFolderIdAndChildSafeTrueOrderByUploadedAtDesc(Long folderId, Pageable pageable);

    Optional<StoredFile> findByOwnerIdAndClientUploadId(Long ownerId, String clientUploadId);

    List<StoredFile> findByFolderId(Long folderId);

    @Modifying
    @Query("UPDATE StoredFile f SET f.childSafe = true WHERE f.folder.id IN :folderIds")
    void markChildSafeByFolderIds(@Param("folderIds") List<Long> folderIds);

    @Modifying
    @Query("UPDATE StoredFile f SET f.childSafe = true WHERE f.id IN :ids")
    void markChildSafeByIds(@Param("ids") List<Long> ids);

    Optional<StoredFile> findByIdAndChildSafe(Long id, boolean childSafe);
}
