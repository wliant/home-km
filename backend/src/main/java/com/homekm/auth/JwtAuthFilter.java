package com.homekm.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homekm.common.ErrorResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final JwtDenylist jwtDenylist;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository,
                         JwtDenylist jwtDenylist, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.jwtDenylist = jwtDenylist;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if ("/api/events".equals(request.getRequestURI())) {
            // SSE: browsers can't set custom headers on EventSource, so accept ?access_token=
            String fromQuery = request.getParameter("access_token");
            if (fromQuery != null && !fromQuery.isBlank()) token = fromQuery;
        }
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (ExpiredJwtException e) {
            writeError(response, 401, "TOKEN_EXPIRED", "Token has expired");
            return;
        } catch (JwtException e) {
            writeError(response, 401, "UNAUTHORIZED", "Invalid token");
            return;
        }

        String jti = claims.getId();
        if (jti != null && jwtDenylist.isRevoked(jti)) {
            writeError(response, 401, "TOKEN_REVOKED", "Token has been revoked");
            return;
        }

        long userId = Long.parseLong(claims.getSubject());
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            writeError(response, 401, "UNAUTHORIZED", "Authentication required");
            return;
        }

        // Quiet hours apply only to child accounts. Window is interpreted in
        // the user's timezone; admin endpoints (handled by an explicit
        // @PreAuthorize) bypass the rest of the filter chain anyway, so an
        // adult helping a child account remains unaffected.
        if (user.isChild() && inQuietHours(user)) {
            writeError(response, 423, "QUIET_HOURS",
                    "This account is in its quiet-hours window. Try again later.");
            return;
        }

        UserPrincipal principal = UserPrincipal.from(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    /**
     * True if the current local time in the user's timezone falls inside
     * the configured quiet-hours window. Window can wrap midnight
     * (start &gt; end means "end the next day"). Either column null = no
     * quiet hours, returns false.
     */
    private boolean inQuietHours(User user) {
        if (user.getQuietHoursStart() == null || user.getQuietHoursEnd() == null) return false;
        java.time.LocalTime now;
        try {
            now = java.time.LocalTime.now(java.time.ZoneId.of(user.getTimezone()));
        } catch (java.time.DateTimeException e) {
            now = java.time.LocalTime.now(java.time.ZoneOffset.UTC);
        }
        java.time.LocalTime s = user.getQuietHoursStart();
        java.time.LocalTime e = user.getQuietHoursEnd();
        if (s.isBefore(e)) {
            return !now.isBefore(s) && now.isBefore(e);
        }
        // Wrap-around window (e.g. 21:00 → 07:00).
        return !now.isBefore(s) || now.isBefore(e);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }
}
