package com.homekm.note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("SELECT n FROM Note n WHERE n.folder.id = :folderId " +
            "ORDER BY n.pinnedAt DESC NULLS LAST, n.updatedAt DESC")
    Page<Note> listByFolder(@Param("folderId") Long folderId, Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.folder IS NULL " +
            "ORDER BY n.pinnedAt DESC NULLS LAST, n.updatedAt DESC")
    Page<Note> listRoot(Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.folder.id = :folderId AND n.childSafe = true " +
            "ORDER BY n.pinnedAt DESC NULLS LAST, n.updatedAt DESC")
    Page<Note> listByFolderChildSafe(@Param("folderId") Long folderId, Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.folder IS NULL AND n.childSafe = true " +
            "ORDER BY n.pinnedAt DESC NULLS LAST, n.updatedAt DESC")
    Page<Note> listRootChildSafe(Pageable pageable);

    @Modifying
    @Query("UPDATE Note n SET n.pinnedAt = :ts WHERE n.id = :id")
    void setPinnedAt(@Param("id") Long id, @Param("ts") Instant ts);

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
