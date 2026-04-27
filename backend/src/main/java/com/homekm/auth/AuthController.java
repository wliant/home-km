package com.homekm.auth;

import com.homekm.auth.dto.*;
import com.homekm.common.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                          LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req,
                                                HttpServletRequest servletRequest) {
        if (!loginRateLimiter.isAllowed(servletRequest.getRemoteAddr())) {
            throw new RateLimitException();
        }
        return ResponseEntity.ok(authService.login(req));
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
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.refreshToken()));
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
}
