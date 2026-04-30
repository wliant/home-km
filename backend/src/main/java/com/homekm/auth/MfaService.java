package com.homekm.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.homekm.common.AppProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * TOTP MFA orchestration. The login challenge cache is the only piece of
 * non-DB state — it's a 5-minute in-memory bridge between the password step
 * (which says "MFA needed") and the code step (which actually issues a JWT).
 * Surviving an API restart isn't a goal: the user just re-enters their password.
 */
@Service
public class MfaService {

    private static final int RECOVERY_CODE_COUNT = 10;
    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository userRepository;
    private final MfaRecoveryCodeRepository recoveryCodeRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    /** challenge token → user id. 5-minute TTL keeps a stolen challenge token from outliving the prompt. */
    private final Cache<String, Long> loginChallenges = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10_000)
            .build();

    public MfaService(UserRepository userRepository,
                      MfaRecoveryCodeRepository recoveryCodeRepository,
                      TotpService totpService,
                      PasswordEncoder passwordEncoder,
                      AppProperties appProperties) {
        this.userRepository = userRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.totpService = totpService;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    public record EnrollmentSecret(String secret, String provisioningUri) {}

    /**
     * Generate a fresh secret and return the otpauth URI for the QR code.
     * Stored on the user but {@code mfa_enabled} stays false until a code is verified.
     */
    @Transactional
    public EnrollmentSecret enroll(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        String secret = totpService.generateBase32Secret();
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        userRepository.save(user);
        return new EnrollmentSecret(secret,
                totpService.provisioningUri(appProperties.getName(), user.getEmail(), secret));
    }

    /**
     * Confirm the user can read codes from their authenticator app, then
     * flip {@code mfa_enabled} on and seed a fresh batch of recovery codes.
     * Returns the cleartext recovery codes — show once and discard.
     */
    @Transactional
    public List<String> verifyEnrollment(Long userId, String code) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getMfaSecret() == null || !totpService.verify(user.getMfaSecret(), code)) {
            throw new IllegalArgumentException("INVALID_TOTP_CODE");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        return regenerateRecoveryCodes(userId);
    }

    @Transactional
    public void disable(Long userId, String passwordConfirmation) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(passwordConfirmation, user.getPasswordHash())) {
            throw new IllegalArgumentException("INVALID_PASSWORD");
        }
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        recoveryCodeRepository.deleteByUserId(userId);
    }

    /** Replace any existing recovery codes — used both at enrollment time and from settings. */
    @Transactional
    public List<String> regenerateRecoveryCodes(Long userId) {
        recoveryCodeRepository.deleteByUserId(userId);
        List<String> plaintext = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String code = randomRecoveryCode();
            plaintext.add(code);
            MfaRecoveryCode entity = new MfaRecoveryCode();
            entity.setUserId(userId);
            entity.setCodeHash(passwordEncoder.encode(code));
            recoveryCodeRepository.save(entity);
        }
        return plaintext;
    }

    /** Park a userId behind a single-use challenge token. */
    public String openChallenge(Long userId) {
        String token = randomChallengeToken();
        loginChallenges.put(token, userId);
        return token;
    }

    /**
     * Resolve a challenge token to its userId once. Pulled from cache on
     * success so a captured token cannot be replayed.
     */
    public Long consumeChallenge(String token) {
        if (token == null) return null;
        Long userId = loginChallenges.getIfPresent(token);
        if (userId != null) loginChallenges.invalidate(token);
        return userId;
    }

    public boolean verifyTotp(User user, String code) {
        return user.getMfaSecret() != null && totpService.verify(user.getMfaSecret(), code);
    }

    /**
     * Try a recovery code; consumes the matching row on success so each
     * code is single-use.
     */
    @Transactional
    public boolean consumeRecoveryCode(Long userId, String code) {
        if (code == null || code.isBlank()) return false;
        for (MfaRecoveryCode entity : recoveryCodeRepository.findUnusedByUserId(userId)) {
            if (passwordEncoder.matches(code, entity.getCodeHash())) {
                entity.setUsedAt(java.time.Instant.now());
                recoveryCodeRepository.save(entity);
                return true;
            }
        }
        return false;
    }

    public long unusedRecoveryCodeCount(Long userId) {
        return recoveryCodeRepository.findUnusedByUserId(userId).size();
    }

    private static String randomRecoveryCode() {
        // 10 hex chars, dash-separated for readability: e.g. "a1b2-c3d4-e5"
        byte[] buf = new byte[5];
        RNG.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; i++) {
            sb.append(String.format("%02x", buf[i]));
            if (i == 1 || i == 3) sb.append('-');
        }
        return sb.toString();
    }

    private static String randomChallengeToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return java.util.HexFormat.of().formatHex(buf);
    }
}
