package com.homekm.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtDenylist {

    private static final Logger log = LoggerFactory.getLogger(JwtDenylist.class);

    private final ConcurrentHashMap<String, Instant> denied = new ConcurrentHashMap<>();

    public void revoke(String jti, Instant expiresAt) {
        denied.put(jti, expiresAt);
        Instant now = Instant.now();
        denied.entrySet().removeIf(e -> e.getValue().isBefore(now));
        log.debug("Revoked access token jti={}, denylist size={}", jti, denied.size());
    }

    public boolean isRevoked(String jti) {
        Instant exp = denied.get(jti);
        if (exp == null) return false;
        if (exp.isBefore(Instant.now())) {
            denied.remove(jti);
            return false;
        }
        return true;
    }

    public int size() {
        return denied.size();
    }
}
