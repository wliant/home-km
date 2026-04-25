package com.homekm.common;

import com.homekm.file.StoredFileRepository;
import com.homekm.folder.FolderRepository;
import com.homekm.note.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChildSafeService {

    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;
    private final StoredFileRepository fileRepository;

    public ChildSafeService(FolderRepository folderRepository,
                             NoteRepository noteRepository,
                             StoredFileRepository fileRepository) {
        this.folderRepository = folderRepository;
        this.noteRepository = noteRepository;
        this.fileRepository = fileRepository;
    }

    /**
     * Mark a folder and all its descendants child-safe, including their notes and files.
     * CS-1: cascade on explicit mark-safe.
     */
    @Transactional
    public void cascadeMarkSafe(Long folderId) {
        List<Long> descendantIds = folderRepository.findDescendantIds(folderId);
        List<Long> allFolderIds = new ArrayList<>(descendantIds);
        allFolderIds.add(folderId);

        folderRepository.markChildSafeByIds(allFolderIds);
        noteRepository.markChildSafeByFolderIds(allFolderIds);
        fileRepository.markChildSafeByFolderIds(allFolderIds);
    }

    /**
     * If an unsafe note or file is added to a safe folder, demote the folder.
     * CS-2: no downward cascade on this demotion.
     */
    @Transactional
    public void demoteFolderIfNeeded(Long folderId, boolean itemIsChildSafe) {
        if (!itemIsChildSafe && folderId != null) {
            folderRepository.updateChildSafe(folderId, false);
        }
    }

    /**
     * When moving an item into a safe folder, the item becomes safe.
     * CS-3: moving safe item into unsafe folder keeps it safe.
     */
    public boolean resolveChildSafeOnMove(boolean currentlySafe, Long destinationFolderId) {
        if (destinationFolderId == null) return currentlySafe;
        return folderRepository.findById(destinationFolderId)
                .map(f -> f.isChildSafe() || currentlySafe)
                .orElse(currentlySafe);
    }
}
