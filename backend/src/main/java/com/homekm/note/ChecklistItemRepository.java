package com.homekm.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByNoteIdOrderBySortOrder(Long noteId);

    long countByNoteId(Long noteId);

    long countByNoteIdAndCheckedTrue(Long noteId);

    void deleteByNoteId(Long noteId);
}
