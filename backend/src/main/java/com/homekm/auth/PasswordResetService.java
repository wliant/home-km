package com.homekm.auth;

import com.homekm.audit.AuditService;
import com.homekm.common.AppProperties;
import com.homekm.common.RequestContextHelper;
import com.homekm.common.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;
    private final AuditService auditService;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository resetTokenRepository,
                                PasswordEncoder passwordEncoder,
                                AppProperties props,
                                AuditService auditService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.auditService = auditService;
    }

    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        if (user == null) {
            return;
        }

        String rawToken = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(TokenHasher.sha256(rawToken));
        resetToken.setUser(user);
        int expiryMinutes = props.getPasswordReset().getTokenExpiryMinutes();
        resetToken.setExpiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        resetTokenRepository.save(resetToken);

        String baseUrl = props.getCors().getAllowedOrigins().split(",")[0].trim();
        String resetLink = baseUrl + "/reset-password?token=" + rawToken;

        if (props.getMail().isEnabled() && mailSender != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.getMail().getFrom());
            message.setTo(user.getEmail());
            message.setSubject(props.getName() + " - Password Reset");
            message.setText("You requested a password reset.\n\n"
                    + "Click the link below to reset your password:\n"
                    + resetLink + "\n\n"
                    + "This link expires in " + expiryMinutes + " minutes.\n\n"
                    + "If you did not request this, you can safely ignore this email.");
            mailSender.send(message);
            log.info("Password reset email sent to {}", user.getEmail());
        } else {
            log.warn("Mail disabled. Password reset token for {}: {}", user.getEmail(), rawToken);
            log.warn("Reset link: {}", resetLink);
        }
    }

    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_WEAK");
        }

        String tokenHash = TokenHasher.sha256(rawToken);
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN"));

        if (resetToken.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RESET_TOKEN_ALREADY_USED");
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RESET_TOKEN_EXPIRED");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        resetTokenRepository.save(resetToken);

        auditService.record(user.getId(), "AUTH_PASSWORD_RESET", "user", String.valueOf(user.getId()),
                null, null, RequestContextHelper.currentRequest());

        log.info("Password reset completed for user {}", user.getEmail());
    }
}
