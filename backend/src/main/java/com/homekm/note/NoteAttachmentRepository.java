package com.homekm.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteAttachmentRepository extends JpaRepository<NoteAttachment, Long> {

    List<NoteAttachment> findByNoteIdOrderByPosition(Long noteId);

    Optional<NoteAttachment> findByNoteIdAndFileId(Long noteId, Long fileId);

    void deleteByNoteIdAndFileId(Long noteId, Long fileId);
}
