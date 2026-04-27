package com.homekm.file;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    Page<StoredFile> findByFolderIdAndDeletedAtIsNullOrderByUploadedAtDesc(Long folderId, Pageable pageable);

    Page<StoredFile> findByFolderIsNullAndDeletedAtIsNullOrderByUploadedAtDesc(Pageable pageable);

    Page<StoredFile> findByFolderIdAndChildSafeTrueAndDeletedAtIsNullOrderByUploadedAtDesc(Long folderId, Pageable pageable);

    Optional<StoredFile> findByOwnerIdAndClientUploadId(Long ownerId, String clientUploadId);

    List<StoredFile> findByFolderId(Long folderId);

    @Modifying
    @Query("UPDATE StoredFile f SET f.childSafe = true WHERE f.folder.id IN :folderIds")
    void markChildSafeByFolderIds(@Param("folderIds") List<Long> folderIds);

    @Modifying
    @Query("UPDATE StoredFile f SET f.childSafe = true WHERE f.id IN :ids")
    void markChildSafeByIds(@Param("ids") List<Long> ids);

    @Query("SELECT f FROM StoredFile f WHERE f.id = :id AND f.childSafe = :childSafe AND f.deletedAt IS NULL")
    Optional<StoredFile> findByIdAndChildSafeAndDeletedAtIsNull(@Param("id") Long id, @Param("childSafe") boolean childSafe);

    @Query("SELECT f FROM StoredFile f WHERE f.deletedAt IS NOT NULL ORDER BY f.deletedAt DESC")
    List<StoredFile> findAllDeleted();

    @Query("SELECT f FROM StoredFile f WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<StoredFile> findActiveById(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM StoredFile f WHERE f.deletedAt IS NOT NULL AND f.deletedAt < :cutoff")
    int purgeDeletedBefore(@Param("cutoff") Instant cutoff);
}
