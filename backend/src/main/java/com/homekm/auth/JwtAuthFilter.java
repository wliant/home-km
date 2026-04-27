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
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
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

        UserPrincipal principal = UserPrincipal.from(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }
}
