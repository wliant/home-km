package com.homekm.note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Page<Note> findByFolderIdOrderByUpdatedAtDesc(Long folderId, Pageable pageable);

    Page<Note> findByFolderIsNullOrderByUpdatedAtDesc(Pageable pageable);

    Page<Note> findByFolderIdAndChildSafeTrueOrderByUpdatedAtDesc(Long folderId, Pageable pageable);

    Page<Note> findByFolderIsNullAndChildSafeTrueOrderByUpdatedAtDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE Note n SET n.childSafe = true WHERE n.folder.id IN :folderIds")
    void markChildSafeByFolderIds(@Param("folderIds") List<Long> folderIds);

    @Modifying
    @Query("UPDATE Note n SET n.childSafe = true WHERE n.id IN :ids")
    void markChildSafeByIds(@Param("ids") List<Long> ids);

    List<Note> findByFolderId(Long folderId);

    @Query("SELECT COUNT(n) FROM Note n WHERE n.folder.id IN :folderIds")
    long countByFolderIds(@Param("folderIds") List<Long> folderIds);

    Optional<Note> findByIdAndChildSafe(Long id, boolean childSafe);
}
