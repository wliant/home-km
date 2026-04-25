package com.homekm.push;

import com.homekm.auth.User;
import com.homekm.auth.UserPrincipal;
import com.homekm.auth.UserRepository;
import com.homekm.common.AppProperties;
import com.homekm.push.dto.PushSubscribeRequest;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    public PushService(PushSubscriptionRepository subscriptionRepository,
                       UserRepository userRepository, AppProperties appProperties) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
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
        AppProperties.Vapid vapid = appProperties.getVapid();
        if (vapid.getPublicKey() == null || vapid.getPublicKey().isBlank()) {
            log.warn("VAPID keys not configured; skipping push delivery");
            return;
        }

        List<PushSubscription> subs = subscriptionRepository.findByUserIdIn(userIds);
        for (PushSubscription sub : subs) {
            try {
                PushService pushService = new PushService(vapid.getPublicKey(), vapid.getPrivateKey(),
                        vapid.getSubject());
                String payload = buildPayload(title, body, url);
                Subscription subscription = new Subscription(sub.getEndpoint(),
                        new Subscription.Keys(sub.getP256dhKey(), sub.getAuthKey()));
                Notification notification = new Notification(subscription, payload);
                pushService.send(notification);
            } catch (Exception e) {
                log.warn("Push delivery failed for endpoint {}: {}", sub.getEndpoint(), e.getMessage());
            }
        }
    }

    private String buildPayload(String title, String body, String url) {
        return "{\"title\":\"" + escape(title) + "\",\"body\":\"" + escape(body) + "\",\"url\":\"" + escape(url) + "\"}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
