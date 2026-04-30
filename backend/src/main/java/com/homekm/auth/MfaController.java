package com.homekm.auth;

import com.homekm.audit.AuditService;
import com.homekm.common.RequestContextHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth/mfa")
public class MfaController {

    private final MfaService mfaService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public MfaController(MfaService mfaService, AuditService auditService, UserRepository userRepository) {
        this.mfaService = mfaService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    public record EnrollResponse(String secret, String provisioningUri) {}
    public record VerifyEnrollRequest(@NotBlank String code) {}
    public record VerifyEnrollResponse(List<String> recoveryCodes) {}
    public record DisableRequest(@NotBlank String password) {}
    public record StatusResponse(boolean enabled, long unusedRecoveryCodes) {}

    /** Generate a fresh TOTP secret + provisioning URI. Doesn't enable MFA yet. */
    @PostMapping("/enroll")
    public ResponseEntity<EnrollResponse> enroll(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("login required");
        var secret = mfaService.enroll(principal.getId());
        return ResponseEntity.ok(new EnrollResponse(secret.secret(), secret.provisioningUri()));
    }

    /** Confirm the user's authenticator works, flip MFA on, and return recovery codes. */
    @PostMapping("/verify")
    public ResponseEntity<VerifyEnrollResponse> verifyEnrollment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyEnrollRequest req) {
        if (principal == null) throw new AccessDeniedException("login required");
        try {
            List<String> codes = mfaService.verifyEnrollment(principal.getId(), req.code());
            auditService.record(principal.getId(), "AUTH_MFA_ENABLED", "user",
                    String.valueOf(principal.getId()), null, null, RequestContextHelper.currentRequest());
            return ResponseEntity.ok(new VerifyEnrollResponse(codes));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody DisableRequest req) {
        if (principal == null) throw new AccessDeniedException("login required");
        try {
            mfaService.disable(principal.getId(), req.password());
            auditService.record(principal.getId(), "AUTH_MFA_DISABLED", "user",
                    String.valueOf(principal.getId()), null, null, RequestContextHelper.currentRequest());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /** Replace the current recovery codes (e.g. after one is used). */
    @PostMapping("/recovery-codes")
    public ResponseEntity<VerifyEnrollResponse> regenerateRecoveryCodes(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("login required");
        return ResponseEntity.ok(new VerifyEnrollResponse(mfaService.regenerateRecoveryCodes(principal.getId())));
    }

    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("login required");
        boolean enabled = userRepository.findById(principal.getId())
                .map(User::isMfaEnabled).orElse(false);
        long unused = mfaService.unusedRecoveryCodeCount(principal.getId());
        return ResponseEntity.ok(new StatusResponse(enabled, unused));
    }
}
