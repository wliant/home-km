package com.homekm.admin;

import com.homekm.admin.dto.CreateUserRequest;
import com.homekm.admin.dto.ResetPasswordRequest;
import com.homekm.admin.dto.UpdateUserRequest;
import com.homekm.auth.RefreshTokenRepository;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminController(AdminService adminService, RefreshTokenRepository refreshTokenRepository) {
        this.adminService = adminService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable long id,
                                                    @Valid @RequestBody UpdateUserRequest req,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(adminService.updateUser(id, req, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable long id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        adminService.deleteUser(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable long id,
                                               @Valid @RequestBody ResetPasswordRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        adminService.resetPassword(id, req, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sessions/revoke")
    @Transactional
    public ResponseEntity<Void> revokeUserSessions(@PathVariable Long id) {
        refreshTokenRepository.revokeAllByUserId(id);
        return ResponseEntity.noContent().build();
    }
}
