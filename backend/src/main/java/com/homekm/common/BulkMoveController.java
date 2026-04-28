package com.homekm.common;

import com.homekm.auth.UserPrincipal;
import com.homekm.file.StoredFile;
import com.homekm.file.StoredFileRepository;
import com.homekm.folder.Folder;
import com.homekm.folder.FolderRepository;
import com.homekm.note.Note;
import com.homekm.note.NoteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class BulkMoveController {

    private final NoteRepository noteRepository;
    private final StoredFileRepository fileRepository;
    private final FolderRepository folderRepository;

    public BulkMoveController(NoteRepository noteRepository, StoredFileRepository fileRepository,
                                FolderRepository folderRepository) {
        this.noteRepository = noteRepository;
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
    }

    @PostMapping("/move")
    @Transactional
    public ResponseEntity<BulkMoveResponse> move(@Valid @RequestBody BulkMoveRequest req,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.isChild()) throw new AccessDeniedException("forbidden");

        Folder dest = null;
        if (req.targetFolderId() != null) {
            dest = folderRepository.findActiveById(req.targetFolderId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FOLDER_NOT_FOUND"));
        }
        int moved = 0;
        for (BulkMoveItem it : req.items()) {
            switch (it.type()) {
                case "note" -> {
                    Note n = noteRepository.findActiveById(it.id()).orElse(null);
                    if (n == null) continue;
                    n.setFolder(dest);
                    noteRepository.save(n);
                    moved++;
                }
                case "file" -> {
                    StoredFile f = fileRepository.findActiveById(it.id()).orElse(null);
                    if (f == null) continue;
                    f.setFolder(dest);
                    fileRepository.save(f);
                    moved++;
                }
                case "folder" -> {
                    Folder f = folderRepository.findActiveById(it.id()).orElse(null);
                    if (f == null) continue;
                    if (dest != null && folderRepository.wouldCreateCycle(it.id(), dest.getId())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CYCLE_DETECTED");
                    }
                    f.setParent(dest);
                    folderRepository.save(f);
                    moved++;
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TYPE");
            }
        }
        return ResponseEntity.ok(new BulkMoveResponse(moved));
    }

    public record BulkMoveItem(@NotBlank @Pattern(regexp = "note|file|folder") String type, @NotNull Long id) {}
    public record BulkMoveRequest(@NotNull List<BulkMoveItem> items, Long targetFolderId) {}
    public record BulkMoveResponse(int moved) {}
}
