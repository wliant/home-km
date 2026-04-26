package com.homekm.auth;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 20;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Deque<Long>> windowMap = new ConcurrentHashMap<>();

    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();
        var timestamps = windowMap.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_ATTEMPTS) return false;
            timestamps.addLast(now);
            return true;
        }
    }
}
