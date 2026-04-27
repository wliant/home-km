package com.homekm.auth;

import com.homekm.audit.AuditService;
import com.homekm.auth.dto.*;
import com.homekm.common.AppProperties;
import com.homekm.common.EntityNotFoundException;
import com.homekm.common.RequestContextHelper;
import com.homekm.common.TokenHasher;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtDenylist jwtDenylist;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final AuditService auditService;

    // Dummy hash to ensure constant-time comparison for unknown emails
    private static final String DUMMY_HASH = "$2a$12$dummy.hash.for.timing.safety.xxxxxxxxxxxxxxxxxxxxxxxxxx";

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService, JwtDenylist jwtDenylist,
                       PasswordEncoder passwordEncoder, AppProperties appProperties,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.jwtDenylist = jwtDenylist;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
        }
        User user = new User();
        user.setEmail(req.email().toLowerCase());
        user.setDisplayName(req.displayName());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        if (userRepository.count() == 0) {
            user.setAdmin(true);
        }
        userRepository.save(user);
        log.info("User registered: {}", user.getEmail());
        auditService.record(user.getId(), "AUTH_REGISTER", "user", String.valueOf(user.getId()),
                null, null, RequestContextHelper.currentRequest());
        String token = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);
        return new LoginResponse(token, refreshToken, jwtService.getExpiry(user), UserResponse.from(user));
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase()).orElse(null);
        String hashToCompare = user != null ? user.getPasswordHash() : DUMMY_HASH;
        boolean matches = passwordEncoder.matches(req.password(), hashToCompare);

        if (user == null || !matches) {
            log.warn("Failed login attempt for email: {}", req.email());
            auditService.record(null, "AUTH_LOGIN_FAILED", "user", null,
                    null, null, RequestContextHelper.currentRequest());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED");
        }
        log.info("User logged in: {}", user.getEmail());
        auditService.record(user.getId(), "AUTH_LOGIN", "user", String.valueOf(user.getId()),
                null, null, RequestContextHelper.currentRequest());
        String token = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);
        return new LoginResponse(token, refreshToken, jwtService.getExpiry(user), UserResponse.from(user));
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        String hash = TokenHasher.sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHashForUpdate(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));

        if (stored.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REVOKED");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED");
        }

        // Rotate: revoke old token, create new one
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED");
        }

        log.debug("Token refreshed for user {}", user.getId());
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = createRefreshToken(user);
        return new LoginResponse(newAccessToken, newRefreshToken, jwtService.getExpiry(user), UserResponse.from(user));
    }

    @Transactional
    public void logout(String accessToken, String rawRefreshToken, Long actorUserId) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hash = TokenHasher.sha256(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
                rt.setRevokedAt(Instant.now());
                refreshTokenRepository.save(rt);
            });
        }
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                String jti = jwtService.extractJti(accessToken);
                Instant exp = jwtService.extractExpiration(accessToken);
                if (jti != null) {
                    jwtDenylist.revoke(jti, exp);
                }
            } catch (JwtException e) {
                log.debug("Could not extract jti during logout: {}", e.getMessage());
            }
        }
        log.info("User logged out: {}", actorUserId);
        auditService.record(actorUserId, "AUTH_LOGOUT", "user",
                actorUserId != null ? String.valueOf(actorUserId) : null,
                null, null, RequestContextHelper.currentRequest());
    }

    public UserResponse getMe(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", principal.getId()));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMe(UserPrincipal principal, UpdateMeRequest req) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", principal.getId()));

        if (req.displayName() != null) {
            user.setDisplayName(req.displayName());
        }

        if (req.currentPassword() != null || req.newPassword() != null) {
            if (req.currentPassword() == null || req.newPassword() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Both currentPassword and newPassword are required to change password");
            }
            if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "WRONG_CURRENT_PASSWORD");
            }
            user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        }

        userRepository.save(user);
        return UserResponse.from(user);
    }

    private String createRefreshToken(User user) {
        String raw = UUID.randomUUID().toString();
        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(TokenHasher.sha256(raw));
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(appProperties.getJwt().getRefreshExpiryDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(rt);
        return raw;
    }
}
