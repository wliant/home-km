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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final AuditService auditService;
    private final JavaMailSender mailSender;

    public InvitationService(InvitationRepository invitationRepository,
                              UserRepository userRepository,
                              AppProperties appProperties,
                              AuditService auditService,
                              @Autowired(required = false) JavaMailSender mailSender) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
        this.auditService = auditService;
        this.mailSender = mailSender;
    }

    public List<Invitation> list() {
        return invitationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public IssuedInvitation create(String email, String role, Long invitedBy) {
        String normalized = email.toLowerCase().trim();
        if (userRepository.existsByEmail(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "USER_EXISTS");
        }
        if (invitationRepository.existsByEmailAndAcceptedAtIsNull(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INVITATION_PENDING");
        }
        String raw = UUID.randomUUID().toString().replace("-", "");
        Invitation inv = new Invitation();
        inv.setEmail(normalized);
        inv.setRole(role == null ? "USER" : role.toUpperCase());
        inv.setTokenHash(TokenHasher.sha256(raw));
        inv.setInvitedBy(invitedBy);
        inv.setExpiresAt(Instant.now().plus(appProperties.getInvitations().getExpiryHours(), ChronoUnit.HOURS));
        invitationRepository.save(inv);

        sendEmail(normalized, raw);
        auditService.record(invitedBy, "INVITATION_CREATED", "invitation", String.valueOf(inv.getId()),
                null, null, RequestContextHelper.currentRequest());
        return new IssuedInvitation(inv, raw);
    }

    @Transactional
    public void revoke(Long id, Long actorId) {
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND"));
        invitationRepository.delete(inv);
        auditService.record(actorId, "INVITATION_REVOKED", "invitation", String.valueOf(id),
                null, null, RequestContextHelper.currentRequest());
    }

    public Invitation verify(String rawToken) {
        Invitation inv = invitationRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND"));
        if (inv.isAccepted()) throw new ResponseStatusException(HttpStatus.GONE, "INVITATION_USED");
        if (inv.isExpired()) throw new ResponseStatusException(HttpStatus.GONE, "INVITATION_EXPIRED");
        return inv;
    }

    @Transactional
    public Invitation accept(String rawToken, Long acceptedByUserId) {
        Invitation inv = verify(rawToken);
        inv.setAcceptedAt(Instant.now());
        inv.setAcceptedBy(acceptedByUserId);
        invitationRepository.save(inv);
        return inv;
    }

    public boolean openRegistrationAllowed() {
        return appProperties.getInvitations().isAllowOpenRegistration();
    }

    private void sendEmail(String email, String rawToken) {
        if (mailSender == null || !appProperties.getMail().isEnabled()) {
            log.info("Mail disabled — invitation token for {} = {}", email, rawToken);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(appProperties.getMail().getFrom());
            msg.setTo(email);
            msg.setSubject("You're invited to " + appProperties.getName());
            msg.setText("You've been invited. Use this token within " + appProperties.getInvitations().getExpiryHours() + "h:\n\n"
                    + rawToken + "\n\nVisit /register and paste the token to create your account.");
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("invitation email failed for {}: {}", email, e.getMessage());
        }
    }

    public record IssuedInvitation(Invitation invitation, String rawToken) {}
}
