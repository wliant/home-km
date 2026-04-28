package com.homekm.common;

import com.homekm.auth.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final List<String> WRITE_METHODS = List.of("POST", "PUT", "PATCH", "DELETE");

    private final AppProperties appProperties;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!appProperties.getRateLimit().isEnabled()) {
            chain.doFilter(req, res);
            return;
        }
        String path = req.getRequestURI();
        String method = req.getMethod();
        for (AppProperties.RateLimit.Rule rule : appProperties.getRateLimit().getRules()) {
            if (!matchesMethod(rule, method)) continue;
            if (!matcher.match(rule.getPathPattern(), path)) continue;
            String key = buildKey(rule, req);
            if (!allow(key, rule)) {
                res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                res.setContentType("application/json");
                res.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests; try again later\"}");
                if (log.isDebugEnabled()) log.debug("rate-limited {} on rule {}", path, rule.getId());
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private boolean matchesMethod(AppProperties.RateLimit.Rule rule, String method) {
        String m = rule.getMethod() == null ? "ANY" : rule.getMethod().toUpperCase();
        if ("ANY".equals(m)) return true;
        if ("ANY_WRITE".equals(m)) return WRITE_METHODS.contains(method);
        return m.equals(method);
    }

    private String buildKey(AppProperties.RateLimit.Rule rule, HttpServletRequest req) {
        String scope = rule.getScope() == null ? "ip" : rule.getScope();
        String subject = "user".equals(scope) ? userKey(req) : req.getRemoteAddr();
        return rule.getId() + "|" + subject;
    }

    private String userKey(HttpServletRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return "u:" + up.getId();
        }
        return "anon:" + req.getRemoteAddr();
    }

    private boolean allow(String key, AppProperties.RateLimit.Rule rule) {
        long now = System.currentTimeMillis();
        long windowMs = rule.getWindowSeconds() * 1000L;
        Deque<Long> q = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
            if (q.size() >= rule.getLimit()) return false;
            q.addLast(now);
            return true;
        }
    }
}
