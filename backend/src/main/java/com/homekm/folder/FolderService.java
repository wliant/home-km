package com.homekm.folder;

import com.homekm.auth.User;
import com.homekm.auth.UserRepository;
import com.homekm.common.ChildSafeService;
import com.homekm.common.ChildAccountWriteException;
import com.homekm.common.EntityNotFoundException;
import com.homekm.file.StoredFileRepository;
import com.homekm.folder.dto.FolderRequest;
import com.homekm.folder.dto.FolderResponse;
import com.homekm.note.NoteRepository;
import com.homekm.auth.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FolderService {

    private static final Logger log = LoggerFactory.getLogger(FolderService.class);
    private static final int MAX_DEPTH = 20;

    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;
    private final StoredFileRepository fileRepository;
    private final UserRepository userRepository;
    private final ChildSafeService childSafeService;

    public FolderService(FolderRepository folderRepository, NoteRepository noteRepository,
                         StoredFileRepository fileRepository, UserRepository userRepository,
                         ChildSafeService childSafeService) {
        this.folderRepository = folderRepository;
        this.noteRepository = noteRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.childSafeService = childSafeService;
    }

    public List<FolderResponse> getTree(UserPrincipal principal) {
        List<Folder> all = folderRepository.findAll();
        if (principal.isChild()) {
            all = all.stream().filter(Folder::isChildSafe).toList();
        }
        return buildTree(all, null);
    }

    private List<FolderResponse> buildTree(List<Folder> all, Long parentId) {
        return all.stream()
                .filter(f -> parentId == null
                        ? f.getParent() == null
                        : f.getParent() != null && f.getParent().getId().equals(parentId))
                .map(f -> FolderResponse.from(f).withChildren(buildTree(all, f.getId())))
                .collect(Collectors.toList());
    }

    public FolderResponse getById(Long id, UserPrincipal principal) {
        Folder folder = findVisibleFolder(id, principal);
        return FolderResponse.from(folder);
    }

    @Transactional
    public FolderResponse create(FolderRequest req, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();

        User owner = userRepository.getReferenceById(principal.getId());
        Folder folder = new Folder();
        folder.setName(req.name());
        folder.setDescription(req.description());
        folder.setOwner(owner);

        if (req.parentId() != null) {
            Folder parent = folderRepository.findById(req.parentId())
                    .orElseThrow(() -> new EntityNotFoundException("Folder", req.parentId()));
            validateDepth(req.parentId());
            validateSiblingName(req.parentId(), req.name(), null);
            folder.setParent(parent);
        } else {
            validateRootName(req.name(), null);
        }

        folderRepository.save(folder);
        return FolderResponse.from(folder);
    }

    @Transactional
    public FolderResponse update(Long id, FolderRequest req, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();

        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Folder", id));

        if (req.name() != null && !req.name().equals(folder.getName())) {
            Long currentParentId = folder.getParent() != null ? folder.getParent().getId() : null;
            if (currentParentId != null) {
                validateSiblingName(currentParentId, req.name(), id);
            } else {
                validateRootName(req.name(), id);
            }
            folder.setName(req.name());
        }

        if (req.description() != null) folder.setDescription(req.description());

        // Handle move
        Long newParentId = req.parentId();
        Long currentParentId = folder.getParent() != null ? folder.getParent().getId() : null;
        boolean parentChanged = !java.util.Objects.equals(newParentId, currentParentId);

        if (parentChanged) {
            if (newParentId != null) {
                if (folderRepository.wouldCreateCycle(id, newParentId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CYCLE_DETECTED");
                }
                validateDepth(newParentId);
                validateSiblingName(newParentId, folder.getName(), id);
                Folder newParent = folderRepository.findById(newParentId)
                        .orElseThrow(() -> new EntityNotFoundException("Folder", newParentId));
                folder.setParent(newParent);
            } else {
                validateRootName(folder.getName(), id);
                folder.setParent(null);
            }
        }

        folderRepository.save(folder);
        return FolderResponse.from(folder);
    }

    @Transactional
    public void delete(Long id, boolean force, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();

        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Folder", id));

        boolean hasChildren = folderRepository.existsByParentId(id);
        if (hasChildren && !force) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FOLDER_HAS_CHILDREN");
        }

        if (force) {
            List<Long> descendantIds = folderRepository.findDescendantIds(id);
            // Delete files in MinIO (best-effort) — log failures
            descendantIds.forEach(did -> {
                fileRepository.findByFolderId(did).forEach(f -> {
                    log.warn("Force delete: skipping MinIO object deletion for key {}", f.getMinioKey());
                });
                fileRepository.deleteAll(fileRepository.findByFolderId(did));
                noteRepository.deleteAll(noteRepository.findByFolderId(did));
            });
            // Delete all descendants (bottom-up)
            for (int i = descendantIds.size() - 1; i >= 0; i--) {
                folderRepository.deleteById(descendantIds.get(i));
            }
        }

        // Delete items at this folder level
        fileRepository.findByFolderId(id).forEach(f ->
                log.warn("Force delete: skipping MinIO object deletion for key {}", f.getMinioKey()));
        fileRepository.deleteAll(fileRepository.findByFolderId(id));
        noteRepository.deleteAll(noteRepository.findByFolderId(id));
        folderRepository.delete(folder);
    }

    @Transactional
    public FolderResponse setChildSafe(Long id, boolean childSafe, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();

        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Folder", id));
        folder.setChildSafe(childSafe);
        folderRepository.save(folder);

        if (childSafe) {
            childSafeService.cascadeMarkSafe(id);
        }

        return FolderResponse.from(folder);
    }

    private Folder findVisibleFolder(Long id, UserPrincipal principal) {
        if (principal.isChild()) {
            return folderRepository.findByIdAndChildSafe(id, true)
                    .orElseThrow(() -> new EntityNotFoundException("Folder", id));
        }
        return folderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Folder", id));
    }

    private void validateDepth(Long parentId) {
        int depth = folderRepository.countAncestorDepth(parentId);
        if (depth >= MAX_DEPTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MAX_FOLDER_DEPTH_EXCEEDED");
        }
    }

    private void validateSiblingName(Long parentId, String name, Long excludeId) {
        List<Folder> siblings = folderRepository.findByParentId(parentId);
        boolean conflict = siblings.stream()
                .filter(f -> excludeId == null || !f.getId().equals(excludeId))
                .anyMatch(f -> f.getName().equalsIgnoreCase(name));
        if (conflict) throw new ResponseStatusException(HttpStatus.CONFLICT, "FOLDER_NAME_EXISTS");
    }

    private void validateRootName(String name, Long excludeId) {
        List<Folder> roots = folderRepository.findByParentIsNull();
        boolean conflict = roots.stream()
                .filter(f -> excludeId == null || !f.getId().equals(excludeId))
                .anyMatch(f -> f.getName().equalsIgnoreCase(name));
        if (conflict) throw new ResponseStatusException(HttpStatus.CONFLICT, "FOLDER_NAME_EXISTS");
    }
}
