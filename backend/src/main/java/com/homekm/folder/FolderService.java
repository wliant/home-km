package com.homekm.folder;

import com.homekm.audit.AuditService;
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
import com.homekm.common.RequestContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
    private final AuditService auditService;

    public FolderService(FolderRepository folderRepository, NoteRepository noteRepository,
                         StoredFileRepository fileRepository, UserRepository userRepository,
                         ChildSafeService childSafeService, AuditService auditService) {
        this.folderRepository = folderRepository;
        this.noteRepository = noteRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.childSafeService = childSafeService;
        this.auditService = auditService;
    }

    @Cacheable(value = "folderTree", key = "#principal.isChild() ? 'child' : 'full'")
    public List<FolderResponse> getTree(UserPrincipal principal) {
        List<Folder> all = folderRepository.findAllActive();
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
        return FolderResponse.from(folder).withAncestors(buildAncestorChain(folder));
    }

    /**
     * Climb the parent chain for a single folder, capped at {@link #MAX_DEPTH}
     * to defend against any cycle that escaped {@code wouldCreateCycle}. Root
     * → folder order. The folder itself is included as the last crumb so the
     * frontend can render either with or without the trailing entry.
     */
    private List<FolderResponse.Crumb> buildAncestorChain(Folder folder) {
        List<FolderResponse.Crumb> chain = new ArrayList<>();
        Folder cursor = folder;
        int safety = 0;
        while (cursor != null && safety++ < MAX_DEPTH + 1) {
            chain.add(new FolderResponse.Crumb(cursor.getId(), cursor.getName()));
            cursor = cursor.getParent();
        }
        Collections.reverse(chain);
        return chain;
    }

    @Transactional
    @CacheEvict(value = "folderTree", allEntries = true)
    public FolderResponse create(FolderRequest req, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();

        User owner = userRepository.getReferenceById(principal.getId());
        Folder folder = new Folder();
        folder.setName(req.name());
        folder.setDescription(req.description());
        folder.setOwner(owner);

        if (req.parentId() != null) {
            Folder parent = folderRepository.findActiveById(req.parentId())
                    .orElseThrow(() -> new EntityNotFoundException("Folder", req.parentId()));
            validateDepth(req.parentId());
            validateSiblingName(req.parentId(), req.name(), null);
            folder.setParent(parent);
        } else {
            validateRootName(req.name(), null);
        }
        if (req.color() != null && !req.color().isBlank()) folder.setColor(req.color());
        if (req.icon() != null && !req.icon().isBlank()) folder.setIcon(req.icon());

        folderRepository.save(folder);
        return FolderResponse.from(folder);
    }

    @Transactional
    @CacheEvict(value = "folderTree", allEntries = true)
    public FolderResponse update(Long id, FolderRequest req, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();

        Folder folder = folderRepository.findActiveById(id)
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
        if (req.color() != null) folder.setColor(req.color().isBlank() ? null : req.color());
        if (req.icon() != null) folder.setIcon(req.icon().isBlank() ? null : req.icon());

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
                Folder newParent = folderRepository.findActiveById(newParentId)
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
    @CacheEvict(value = "folderTree", allEntries = true)
    public void delete(Long id, boolean force, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();

        Folder folder = folderRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Folder", id));

        boolean hasChildren = folderRepository.existsByParentId(id);
        if (hasChildren && !force) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FOLDER_HAS_CHILDREN");
        }

        Instant now = Instant.now();

        if (force) {
            List<Long> descendantIds = folderRepository.findDescendantIds(id);
            // Soft-delete files and notes in descendant folders
            descendantIds.forEach(did -> {
                fileRepository.findByFolderId(did).forEach(f -> {
                    f.setDeletedAt(now);
                    fileRepository.save(f);
                });
                noteRepository.findByFolderId(did).forEach(n -> {
                    n.setDeletedAt(now);
                    noteRepository.save(n);
                });
            });
            // Soft-delete all descendant folders
            for (Long did : descendantIds) {
                folderRepository.findById(did).ifPresent(d -> {
                    d.setDeletedAt(now);
                    folderRepository.save(d);
                });
            }
        }

        // Soft-delete items at this folder level
        fileRepository.findByFolderId(id).forEach(f -> {
            f.setDeletedAt(now);
            fileRepository.save(f);
        });
        noteRepository.findByFolderId(id).forEach(n -> {
            n.setDeletedAt(now);
            noteRepository.save(n);
        });
        folder.setDeletedAt(now);
        folderRepository.save(folder);
        auditService.record(principal.getId(), "FOLDER_DELETE", "folder", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
    }

    @Transactional
    @CacheEvict(value = "folderTree", allEntries = true)
    public FolderResponse archive(Long id, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Folder folder = folderRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Folder", id));
        folder.setArchivedAt(Instant.now());
        folderRepository.save(folder);
        auditService.record(principal.getId(), "FOLDER_ARCHIVE", "folder", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
        return FolderResponse.from(folder);
    }

    @Transactional
    @CacheEvict(value = "folderTree", allEntries = true)
    public FolderResponse unarchive(Long id, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Folder", id));
        if (folder.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FOLDER_DELETED");
        }
        folder.setArchivedAt(null);
        folderRepository.save(folder);
        auditService.record(principal.getId(), "FOLDER_UNARCHIVE", "folder", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
        return FolderResponse.from(folder);
    }

    public List<FolderResponse> listArchived(UserPrincipal principal) {
        if (principal.isChild()) return List.of();
        return folderRepository.findAllArchived().stream()
                .map(FolderResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "folderTree", allEntries = true)
    public void restore(Long id, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Folder", id));
        if (folder.getDeletedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NOT_DELETED");
        }
        folder.setDeletedAt(null);
        folderRepository.save(folder);
        auditService.record(principal.getId(), "FOLDER_RESTORE", "folder", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
    }

    @Transactional
    @CacheEvict(value = "folderTree", allEntries = true)
    public FolderResponse setChildSafe(Long id, boolean childSafe, UserPrincipal principal) {
        if (principal.isChild()) throw new ChildAccountWriteException();

        Folder folder = folderRepository.findActiveById(id)
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
            return folderRepository.findByIdAndChildSafeAndDeletedAtIsNull(id, true)
                    .orElseThrow(() -> new EntityNotFoundException("Folder", id));
        }
        return folderRepository.findActiveById(id)
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
