package com.homekm.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only roster of household members. Available to every authenticated
 * user (not admin-gated like {@code /api/admin/users}) so feature UIs that
 * need a "pick a person" affordance — ACLs, mentions, group editing — can
 * resolve display names without leaking sensitive fields.
 *
 * Returns minimal identity only: id, displayName, isChild, isActive. Email
 * and password-related fields stay on the admin endpoint.
 */
@RestController
@RequestMapping("/api/users")
public class UserRosterController {

    public record RosterEntry(long id, String displayName, boolean isChild, boolean isActive) {}

    private final UserRepository userRepository;

    public UserRosterController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<RosterEntry>> roster(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.ok(List.of());
        List<RosterEntry> roster = userRepository.findAll().stream()
                .filter(User::isActive)
                .map(u -> new RosterEntry(u.getId(), u.getDisplayName(), u.isChild(), u.isActive()))
                .toList();
        return ResponseEntity.ok(roster);
    }
}
