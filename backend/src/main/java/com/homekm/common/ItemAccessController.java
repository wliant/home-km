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
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/items")
public class ItemAccessController {

    private static final Set<String> TYPES = Set.of("note", "file", "folder");

    private final NoteRepository noteRepository;
    private final StoredFileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final AccessControlService acs;

    public ItemAccessController(NoteRepository noteRepository,
                                  StoredFileRepository fileRepository,
                                  FolderRepository folderRepository,
                                  AccessControlService acs) {
        this.noteRepository = noteRepository;
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.acs = acs;
    }

    @PutMapping("/{type}/{id}/visibility")
    @Transactional
    public ResponseEntity<VisibilityResponse> setVisibility(@PathVariable String type, @PathVariable Long id,
                                                             @Valid @RequestBody VisibilityRequest req,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        if (!TYPES.contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TYPE");
        if (principal == null || principal.isChild()) throw new AccessDeniedException("forbidden");
        ResolvedItem item = resolve(type, id);
        if (!principal.isAdmin() && (item.ownerId == null || !item.ownerId.equals(principal.getId()))) {
            throw new AccessDeniedException("only owner or admin can change visibility");
        }
        Visibility v = Visibility.fromDb(req.visibility());
        switch (type) {
            case "note" -> {
                Note n = (Note) item.entity;
                n.setVisibility(v.dbValue());
                noteRepository.save(n);
            }
            case "file" -> {
                StoredFile f = (StoredFile) item.entity;
                f.setVisibility(v.dbValue());
                fileRepository.save(f);
            }
            case "folder" -> {
                Folder f = (Folder) item.entity;
                f.setVisibility(v.dbValue());
                folderRepository.save(f);
            }
        }
        if (req.acls() != null) {
            acs.replaceAcls(type, id, req.acls().stream()
                    .map(a -> new AccessControlService.AclEntry(a.userId(), a.role()))
                    .toList());
        }
        return ResponseEntity.ok(new VisibilityResponse(v.dbValue(),
                acs.listAcls(type, id).stream().map(a -> new AclEntry(a.getUserId(), a.getRole())).toList()));
    }

    @GetMapping("/{type}/{id}/visibility")
    public ResponseEntity<VisibilityResponse> getVisibility(@PathVariable String type, @PathVariable Long id,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        if (!TYPES.contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TYPE");
        ResolvedItem item = resolve(type, id);
        Visibility v = Visibility.fromDb(item.visibility);
        if (!acs.canRead(type, id, item.ownerId, v, principal)) throw new AccessDeniedException("forbidden");
        return ResponseEntity.ok(new VisibilityResponse(v.dbValue(),
                acs.listAcls(type, id).stream().map(a -> new AclEntry(a.getUserId(), a.getRole())).toList()));
    }

    private ResolvedItem resolve(String type, Long id) {
        return switch (type) {
            case "note" -> noteRepository.findById(id).map(n ->
                    new ResolvedItem(n, n.getOwner() != null ? n.getOwner().getId() : null, n.getVisibility()))
                    .orElseThrow(() -> new EntityNotFoundException("Note", id));
            case "file" -> fileRepository.findById(id).map(f ->
                    new ResolvedItem(f, f.getOwner() != null ? f.getOwner().getId() : null, f.getVisibility()))
                    .orElseThrow(() -> new EntityNotFoundException("File", id));
            case "folder" -> folderRepository.findById(id).map(f ->
                    new ResolvedItem(f, f.getOwner() != null ? f.getOwner().getId() : null, f.getVisibility()))
                    .orElseThrow(() -> new EntityNotFoundException("Folder", id));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TYPE");
        };
    }

    private record ResolvedItem(Object entity, Long ownerId, String visibility) {}

    public record VisibilityRequest(
            @NotBlank @Pattern(regexp = "private|household|custom") String visibility,
            List<AclEntry> acls
    ) {}

    public record AclEntry(Long userId, String role) {}

    public record VisibilityResponse(String visibility, List<AclEntry> acls) {}
}
