package com.homekm.auth;

import com.homekm.audit.AuditService;
import com.homekm.common.AppProperties;
import com.homekm.common.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    private AppProperties props;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        props = new AppProperties();
        props.getPasswordReset().setTokenExpiryMinutes(60);
        props.getCors().setAllowedOrigins("http://localhost:3000");
        props.getMail().setEnabled(false);
        props.setName("Home KM");
        // Set a valid JWT secret so AppProperties doesn't complain if validated
        props.getJwt().setSecret("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");

        service = new PasswordResetService(userRepository, resetTokenRepository,
                passwordEncoder, props, auditService);
    }

    @Test
    void requestReset_silentlyReturnsIfUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Should not throw
        service.requestReset("unknown@example.com");

        verify(resetTokenRepository, never()).save(any());
    }

    @Test
    void confirmReset_rejectsTooWeakPassword() {
        assertThatThrownBy(() -> service.confirmReset("some-token", "weak"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PASSWORD_TOO_WEAK");
    }

    @Test
    void confirmReset_rejectsExpiredToken() {
        String rawToken = "test-raw-token";
        String tokenHash = TokenHasher.sha256(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        resetToken.setUsedAt(null);

        when(resetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> service.confirmReset(rawToken, "StrongPass1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("RESET_TOKEN_EXPIRED");
    }

    @Test
    void confirmReset_rejectsAlreadyUsedToken() {
        String rawToken = "test-raw-token";
        String tokenHash = TokenHasher.sha256(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetToken.setUsedAt(Instant.now().minus(10, ChronoUnit.MINUTES));

        when(resetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> service.confirmReset(rawToken, "StrongPass1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("RESET_TOKEN_ALREADY_USED");
    }

    @Test
    void confirmReset_updatesPasswordAndMarksTokenUsed() {
        String rawToken = "test-raw-token";
        String tokenHash = TokenHasher.sha256(rawToken);

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setEmail("alice@example.com");

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetToken.setUsedAt(null);
        resetToken.setUser(user);

        when(resetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("StrongPass1")).thenReturn("encoded-hash");

        service.confirmReset(rawToken, "StrongPass1");

        // Verify password was updated
        assertThat(user.getPasswordHash()).isEqualTo("encoded-hash");
        verify(userRepository).save(user);

        // Verify token was marked as used
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUsedAt()).isNotNull();

        // Verify audit event was recorded
        verify(auditService).record(eq(7L), eq("AUTH_PASSWORD_RESET"), eq("user"), eq("7"),
                isNull(), isNull(), any());
    }
}
