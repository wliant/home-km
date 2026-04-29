package com.homekm.auth;

import com.homekm.auth.dto.*;
import com.homekm.common.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final InvitationService invitationService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                          InvitationService invitationService,
                          LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.invitationService = invitationService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest req,
                                                   HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req, servletRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req,
                                                HttpServletRequest servletRequest) {
        if (!loginRateLimiter.isAllowed(servletRequest.getRemoteAddr())) {
            throw new RateLimitException();
        }
        return ResponseEntity.ok(authService.login(req, servletRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getMe(principal));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody UpdateMeRequest req) {
        return ResponseEntity.ok(authService.updateMe(principal, req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req,
                                                  HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.refresh(req.refreshToken(), servletRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestBody(required = false) LogoutRequest req,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String accessToken = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : null;
        String refreshToken = req != null ? req.refreshToken() : null;
        authService.logout(accessToken, refreshToken, principal != null ? principal.getId() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest req,
                                                      HttpServletRequest servletRequest) {
        if (!loginRateLimiter.isAllowed(servletRequest.getRemoteAddr())) {
            throw new RateLimitException();
        }
        passwordResetService.requestReset(req.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        passwordResetService.confirmReset(req.token(), req.newPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> sessions(@AuthenticationPrincipal UserPrincipal principal,
                                                           @RequestHeader(value = "X-Refresh-Token", required = false) String currentRefreshToken) {
        if (principal == null) throw new AccessDeniedException("login required");
        return ResponseEntity.ok(authService.listSessions(principal.getId(), currentRefreshToken));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revokeSession(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id) {
        if (principal == null) throw new AccessDeniedException("login required");
        authService.revokeSession(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitations/{token}")
    public ResponseEntity<InvitationResponse> verifyInvitation(@PathVariable String token) {
        return ResponseEntity.ok(InvitationResponse.from(invitationService.verify(token)));
    }

    public record DeleteAccountRequest(@jakarta.validation.constraints.NotBlank String password) {}

    /**
     * Self-service account deactivation. Returns 401 on bad password, 409
     * if the caller is the last active admin, 204 on success.
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody DeleteAccountRequest req) {
        if (principal == null) throw new AccessDeniedException("login required");
        authService.deactivateSelf(principal.getId(), req.password());
        return ResponseEntity.noContent().build();
    }
}
