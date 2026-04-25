package com.homekm.auth;

import com.homekm.auth.dto.*;
import com.homekm.common.AppProperties;
import com.homekm.common.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    // Dummy hash to ensure constant-time comparison for unknown emails
    private static final String DUMMY_HASH = "$2a$12$dummy.hash.for.timing.safety.xxxxxxxxxxxxxxxxxxxxxxxxxx";

    public AuthService(UserRepository userRepository, JwtService jwtService,
                       PasswordEncoder passwordEncoder, AppProperties appProperties) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
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
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, jwtService.getExpiry(user), UserResponse.from(user));
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase()).orElse(null);
        String hashToCompare = user != null ? user.getPasswordHash() : DUMMY_HASH;
        boolean matches = passwordEncoder.matches(req.password(), hashToCompare);

        if (user == null || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED");
        }
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, jwtService.getExpiry(user), UserResponse.from(user));
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
}
