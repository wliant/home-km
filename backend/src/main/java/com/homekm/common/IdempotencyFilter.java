package com.homekm.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homekm.auth.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String HEADER = "Idempotency-Key";
    private static final List<String> METHODS = List.of("POST", "PUT", "PATCH");

    private final AppProperties appProperties;
    private final IdempotencyKeyRepository repository;
    private final ObjectMapper mapper;

    public IdempotencyFilter(AppProperties appProperties,
                              IdempotencyKeyRepository repository,
                              @Autowired(required = false) ObjectMapper mapper) {
        this.appProperties = appProperties;
        this.repository = repository;
        this.mapper = mapper != null ? mapper : new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!appProperties.getIdempotency().isEnabled()) {
            chain.doFilter(req, res);
            return;
        }
        String key = req.getHeader(HEADER);
        if (key == null || !METHODS.contains(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        Long userId = currentUserId();
        String keyHash = sha256(key);
        byte[] bodyBytes = req.getInputStream().readAllBytes();
        String reqHash = sha256(bodyBytes);
        String path = req.getRequestURI();

        var existing = repository.findExisting(keyHash, userId, req.getMethod(), path);
        if (existing.isPresent()) {
            IdempotencyKey rec = existing.get();
            if (!rec.getRequestHash().equals(reqHash)) {
                res.setStatus(HttpServletResponse.SC_CONFLICT);
                res.setContentType("application/json");
                res.getWriter().write("{\"code\":\"IDEMPOTENCY_KEY_REUSED\",\"message\":\"Key already used with a different request body\"}");
                return;
            }
            res.setStatus(rec.getStatusCode());
            if (rec.getResponseContentType() != null) res.setContentType(rec.getResponseContentType());
            if (rec.getResponseBody() != null) res.getWriter().write(rec.getResponseBody());
            return;
        }

        CachedBodyRequest wrapped = new CachedBodyRequest(req, bodyBytes);
        CapturingResponse capturing = new CapturingResponse(res);
        chain.doFilter(wrapped, capturing);
        capturing.flushBuffer();

        if (capturing.getStatus() >= 200 && capturing.getStatus() < 300) {
            try {
                IdempotencyKey rec = new IdempotencyKey();
                rec.setKeyHash(keyHash);
                rec.setUserId(userId);
                rec.setMethod(req.getMethod());
                rec.setPath(path);
                rec.setRequestHash(reqHash);
                rec.setStatusCode(capturing.getStatus());
                rec.setResponseBody(capturing.getCapturedBody());
                rec.setResponseContentType(capturing.getContentType());
                rec.setExpiresAt(Instant.now().plusSeconds(appProperties.getIdempotency().getTtlHours() * 3600L));
                repository.save(rec);
            } catch (Exception e) {
                log.warn("Failed to record idempotency key: {}", e.getMessage());
            }
        }
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) return up.getId();
        return null;
    }

    private static String sha256(String s) {
        return sha256(s.getBytes());
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedBodyRequest(HttpServletRequest req, byte[] body) { super(req); this.body = body; }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener l) {}
                @Override public int read() { return bais.read(); }
            };
        }
    }

    private static class CapturingResponse extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final PrintWriter writer = new PrintWriter(buffer);
        private final jakarta.servlet.ServletOutputStream stream = new jakarta.servlet.ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener l) {}
            @Override public void write(int b) { buffer.write(b); }
        };

        CapturingResponse(HttpServletResponse delegate) { super(delegate); }

        @Override public PrintWriter getWriter() { return writer; }
        @Override public jakarta.servlet.ServletOutputStream getOutputStream() { return stream; }

        @Override
        public void flushBuffer() throws IOException {
            writer.flush();
            byte[] captured = buffer.toByteArray();
            if (captured.length > 0) {
                getResponse().getOutputStream().write(captured);
                getResponse().getOutputStream().flush();
            }
        }

        public String getCapturedBody() {
            writer.flush();
            return buffer.toString();
        }
    }
}
