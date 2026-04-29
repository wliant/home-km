package com.homekm.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    @Query("SELECT v FROM FileVersion v WHERE v.file.id = :fileId ORDER BY v.uploadedAt DESC")
    List<FileVersion> findByFileId(@Param("fileId") Long fileId);

    @Query("SELECT v FROM FileVersion v WHERE v.file.id = :fileId AND v.current = true")
    Optional<FileVersion> findCurrent(@Param("fileId") Long fileId);

    @Modifying
    @Query("UPDATE FileVersion v SET v.current = false WHERE v.file.id = :fileId AND v.current = true")
    void clearCurrent(@Param("fileId") Long fileId);
}
