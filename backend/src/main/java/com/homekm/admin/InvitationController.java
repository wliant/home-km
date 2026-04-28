package com.homekm.admin;

import com.homekm.auth.InvitationService;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.dto.CreateInvitationRequest;
import com.homekm.auth.dto.InvitationResponse;
import com.homekm.auth.dto.IssuedInvitationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(invitationService.list().stream().map(InvitationResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<IssuedInvitationResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                            @Valid @RequestBody CreateInvitationRequest req) {
        requireAdmin(principal);
        var issued = invitationService.create(req.email(), req.role(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(IssuedInvitationResponse.from(issued));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        requireAdmin(principal);
        invitationService.revoke(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(UserPrincipal principal) {
        if (principal == null || !principal.isAdmin()) {
            throw new AccessDeniedException("admin only");
        }
    }
}
