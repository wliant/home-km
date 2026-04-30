package com.homekm.group;

import com.homekm.auth.UserPrincipal;
import com.homekm.group.dto.GroupRequest;
import com.homekm.group.dto.GroupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService service;

    public GroupController(GroupService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupRequest req,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody GroupRequest req,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static void requireAdmin(UserPrincipal principal) {
        if (principal == null || !principal.isAdmin()) {
            throw new AccessDeniedException("admin only");
        }
    }
}
