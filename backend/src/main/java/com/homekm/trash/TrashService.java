package com.homekm.trash;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.ChildAccountWriteException;
import com.homekm.file.StoredFileRepository;
import com.homekm.folder.FolderRepository;
import com.homekm.note.NoteRepository;
import org.springframework.stereotype.Service;

@Service
public class TrashService {

    private final NoteRepository noteRepository;
    private final StoredFileRepository fileRepository;
    private final FolderRepository folderRepository;

    public TrashService(NoteRepository noteRepository, StoredFileRepository fileRepository,
                        FolderRepository folderRepository) {
        this.noteRepository = noteRepository;
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
    }

    public TrashResponse getTrash(UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();

        var notes = noteRepository.findAllDeleted().stream()
                .map(n -> new TrashResponse.TrashItem(n.getId(), "note", n.getTitle(), n.getDeletedAt()))
                .toList();
        var files = fileRepository.findAllDeleted().stream()
                .map(f -> new TrashResponse.TrashItem(f.getId(), "file", f.getFilename(), f.getDeletedAt()))
                .toList();
        var folders = folderRepository.findAllDeleted().stream()
                .map(f -> new TrashResponse.TrashItem(f.getId(), "folder", f.getName(), f.getDeletedAt()))
                .toList();
        return new TrashResponse(notes, files, folders);
    }
}
