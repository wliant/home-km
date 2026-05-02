package com.homekm.push;

import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.common.AppProperties;
import com.homekm.push.dto.PushSubscribeRequest;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final com.homekm.auth.QuietHoursService quietHours;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    public PushService(PushSubscriptionRepository subscriptionRepository,
                       UserRepository userRepository, AppProperties appProperties,
                       com.homekm.auth.QuietHoursService quietHours) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
        this.quietHours = quietHours;
    }

    public String getVapidPublicKey() {
        return appProperties.getVapid().getPublicKey();
    }

    @Transactional
    public void subscribe(PushSubscribeRequest req, UserPrincipal principal) {
        subscriptionRepository.findByEndpoint(req.endpoint()).ifPresent(subscriptionRepository::delete);
        User user = userRepository.getReferenceById(principal.getId());
        PushSubscription sub = new PushSubscription();
        sub.setUser(user);
        sub.setEndpoint(req.endpoint());
        sub.setP256dhKey(req.p256dhKey());
        sub.setAuthKey(req.authKey());
        sub.setUserAgent(req.userAgent());
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
    }

    public void sendToUsers(List<Long> userIds, String title, String body, String url) {
        sendToUsers(userIds, title, body, url, null);
    }

    /**
     * Drop user IDs whose {@code notification_prefs.reminders} is explicitly
     * false. Absent or {@code true} means push is allowed (default-on so
     * existing users keep getting reminders without opting in to anything).
     */
    private List<Long> filterByReminderPref(List<Long> userIds) {
        if (userIds.isEmpty()) return userIds;
        List<User> users = userRepository.findAllById(userIds);
        java.util.List<Long> kept = new java.util.ArrayList<>(users.size());
        for (User u : users) {
            String prefs = u.getNotificationPrefs();
            if (prefs == null || prefs.isBlank()
                    || !prefs.contains("\"reminders\"")
                    || prefs.contains("\"reminders\":true")) {
                kept.add(u.getId());
            }
        }
        return kept;
    }

    /**
     * Drop users currently inside their configured quiet-hours window.
     * v1 semantics: skipped notifications are not deferred — the user
     * accepted "don't bother me from X to Y" by setting the window. Same
     * filter applies to email fallback so the whole notification surface
     * goes silent during the window.
     */
    private List<Long> filterByQuietHours(List<Long> userIds) {
        if (userIds.isEmpty()) return userIds;
        Instant now = Instant.now();
        List<User> users = userRepository.findAllById(userIds);
        java.util.List<Long> kept = new java.util.ArrayList<>(users.size());
        for (User u : users) {
            if (!quietHours.isQuiet(u, now)) kept.add(u.getId());
        }
        return kept;
    }

    /**
     * Send a push to every device subscribed by any user in {@code userIds}.
     * When {@code reminderId} is non-null it is embedded in the payload so the
     * service worker can render Done / Snooze action buttons that POST back to
     * /api/reminders/{id}/done and /api/reminders/{id}/snooze.
     */
    public void sendToUsers(List<Long> userIds, String title, String body, String url, Long reminderId) {
        AppProperties.Vapid vapid = appProperties.getVapid();
        boolean pushReady = vapid.getPublicKey() != null && !vapid.getPublicKey().isBlank();
        if (!pushReady) {
            log.warn("VAPID keys not configured; skipping push delivery");
        }

        // Reminder pushes (reminderId != null) honour user prefs; other
        // call paths bypass — they're either critical (security) or already
        // gated by the caller.
        List<Long> targets = reminderId != null ? filterByReminderPref(userIds) : userIds;
        if (reminderId != null) targets = filterByQuietHours(targets);
        if (targets.isEmpty()) return;

        // Track whether each user got at least one successful push so we can
        // decide on email fallback. Only relevant for reminder dispatches.
        List<User> targetUsers = userRepository.findAllById(targets);
        java.util.Map<Long, Boolean> pushDelivered = new java.util.HashMap<>();
        for (Long uid : targets) pushDelivered.put(uid, false);

        if (pushReady) {
            for (User user : targetUsers) {
                List<PushSubscription> subs = subscriptionRepository.findByUserIdIn(List.of(user.getId()));
                for (PushSubscription sub : subs) {
                    try {
                        nl.martijndwars.webpush.PushService pushService = new nl.martijndwars.webpush.PushService(
                                vapid.getPublicKey(), vapid.getPrivateKey(), vapid.getSubject());
                        String payload = buildPayload(title, body, url, reminderId);
                        Subscription subscription = new Subscription(sub.getEndpoint(),
                                new Subscription.Keys(sub.getP256dhKey(), sub.getAuthKey()));
                        Notification notification = new Notification(subscription, payload);
                        pushService.send(notification);
                        pushDelivered.put(user.getId(), true);
                    } catch (Exception e) {
                        log.warn("Push delivery failed for endpoint {}: {}", sub.getEndpoint(), e.getMessage());
                    }
                }
            }
        }

        // Email fallback: only for reminder dispatches, only when push didn't
        // land for that user, only when SMTP is configured, and only when the
        // user hasn't opted out of email-reminders.
        if (reminderId != null && mailSender != null && appProperties.getMail().isEnabled()) {
            for (User user : targetUsers) {
                if (pushDelivered.get(user.getId())) continue;
                if (!emailRemindersEnabled(user)) continue;
                try {
                    org.springframework.mail.SimpleMailMessage msg = new org.springframework.mail.SimpleMailMessage();
                    msg.setFrom(appProperties.getMail().getFrom());
                    msg.setTo(user.getEmail());
                    msg.setSubject(title);
                    String origins = appProperties.getCors().getAllowedOrigins();
                    String baseUrl = origins != null && !origins.isBlank()
                            ? origins.split(",")[0].trim() : "";
                    msg.setText(body + "\n\n" + baseUrl + url + "\n\n"
                            + "(You're receiving this because push delivery did not reach any of your devices."
                            + " Disable email reminders in Settings → Push notifications.)");
                    mailSender.send(msg);
                    log.info("Sent email fallback for reminder {} to {}", reminderId, user.getEmail());
                } catch (Exception e) {
                    log.warn("Email fallback failed for {}: {}", user.getEmail(), e.getMessage());
                }
            }
        }
    }

    /**
     * {@code notification_prefs.emailReminders} defaults to true so users who
     * never enabled push still get the fallback when push delivery fails.
     * Setting it explicitly false in the JSON prefs opts out.
     */
    private boolean emailRemindersEnabled(User user) {
        String prefs = user.getNotificationPrefs();
        if (prefs == null || prefs.isBlank()) return true;
        return !prefs.contains("\"emailReminders\":false");
    }

    private String buildPayload(String title, String body, String url, Long reminderId) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"title\":\"").append(escape(title)).append("\",");
        sb.append("\"body\":\"").append(escape(body)).append("\",");
        sb.append("\"url\":\"").append(escape(url)).append("\"");
        if (reminderId != null) sb.append(",\"reminderId\":").append(reminderId);
        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
