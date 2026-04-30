package com.homekm.group;

import com.homekm.auth.User;
import com.homekm.auth.UserRepository;
import com.homekm.common.EntityNotFoundException;
import com.homekm.group.dto.GroupRequest;
import com.homekm.group.dto.GroupResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages user groups. System groups (Everyone/Adults/Kids) can be listed and
 * expanded to user IDs but never edited — their membership is computed each
 * time from {@link UserRepository}.
 */
@Service
public class GroupService {

    private final UserGroupRepository repository;
    private final UserRepository userRepository;

    public GroupService(UserGroupRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> list() {
        return repository.findAll().stream()
                .map(g -> toResponse(g, expandToUserIds(g)))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse getById(Long id) {
        UserGroup g = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Group", id));
        return toResponse(g, expandToUserIds(g));
    }

    @Transactional
    public GroupResponse create(GroupRequest req) {
        if (repository.existsByName(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "GROUP_NAME_TAKEN");
        }
        UserGroup g = new UserGroup();
        g.setName(req.name());
        g.setKind(UserGroup.Kind.CUSTOM);
        g.setSystem(false);
        applyMembers(g, req.memberUserIds());
        repository.save(g);
        return toResponse(g, expandToUserIds(g));
    }

    @Transactional
    public GroupResponse update(Long id, GroupRequest req) {
        UserGroup g = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Group", id));
        if (g.isSystem()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SYSTEM_GROUP_IMMUTABLE");
        }
        if (!g.getName().equals(req.name()) && repository.existsByName(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "GROUP_NAME_TAKEN");
        }
        g.setName(req.name());
        applyMembers(g, req.memberUserIds());
        repository.save(g);
        return toResponse(g, expandToUserIds(g));
    }

    @Transactional
    public void delete(Long id) {
        UserGroup g = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Group", id));
        if (g.isSystem()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SYSTEM_GROUP_IMMUTABLE");
        }
        repository.delete(g);
    }

    /**
     * Resolve a group to its current set of user IDs. System groups query the
     * users table on every call so the answer reflects the live household.
     */
    public List<Long> expandToUserIds(UserGroup group) {
        return switch (group.getKind()) {
            case SYSTEM_EVERYONE -> userRepository.findActiveUserIds();
            case SYSTEM_ADULTS   -> userRepository.findActiveUserIdsByChild(false);
            case SYSTEM_KIDS     -> userRepository.findActiveUserIdsByChild(true);
            case CUSTOM          -> group.getMembers().stream().map(User::getId).toList();
        };
    }

    /** Convenience: expand a list of group IDs to a deduped set of user IDs. */
    public Set<Long> expandGroupsToUserIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) return Set.of();
        Set<Long> out = new HashSet<>();
        for (Long gid : groupIds) {
            UserGroup g = repository.findById(gid)
                    .orElseThrow(() -> new EntityNotFoundException("Group", gid));
            out.addAll(expandToUserIds(g));
        }
        return out;
    }

    private void applyMembers(UserGroup group, List<Long> userIds) {
        Set<User> members = new HashSet<>();
        if (userIds != null) {
            for (Long uid : userIds) {
                User u = userRepository.findById(uid)
                        .orElseThrow(() -> new EntityNotFoundException("User", uid));
                members.add(u);
            }
        }
        group.getMembers().clear();
        group.getMembers().addAll(members);
    }

    private static GroupResponse toResponse(UserGroup g, List<Long> memberIds) {
        return new GroupResponse(
                g.getId(),
                g.getName(),
                g.getKind().name(),
                g.isSystem(),
                memberIds,
                g.getCreatedAt());
    }
}
