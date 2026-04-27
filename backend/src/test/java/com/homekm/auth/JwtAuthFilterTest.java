package com.homekm.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    JwtService jwtService = mock(JwtService.class);
    UserRepository userRepository = mock(UserRepository.class);
    JwtDenylist jwtDenylist = mock(JwtDenylist.class);
    ObjectMapper objectMapper = new ObjectMapper();
    JwtAuthFilter filter;

    User activeUser;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, userRepository, jwtDenylist, objectMapper);
        SecurityContextHolder.clearContext();

        activeUser = new User();
        ReflectionTestUtils.setField(activeUser, "id", 1L);
        activeUser.setEmail("alice@example.com");
        activeUser.setActive(true);
    }

    @Test
    void noAuthHeader_passesThrough() throws Exception {
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void nonBearerHeader_passesThrough() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic abc123");
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    void expiredToken_returns401WithTokenExpiredCode() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer expiredtoken");
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        when(jwtService.parseToken("expiredtoken"))
            .thenThrow(new ExpiredJwtException(null, null, "token expired"));

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        var body = objectMapper.readTree(res.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("TOKEN_EXPIRED");
        verifyNoInteractions(chain);
    }

    @Test
    void invalidToken_returns401WithUnauthorizedCode() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer badtoken");
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        when(jwtService.parseToken("badtoken"))
            .thenThrow(new JwtException("invalid signature"));

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        var body = objectMapper.readTree(res.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(chain);
    }

    @Test
    void deactivatedUser_returns401() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer validtoken");
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        var claims = mock(Claims.class);

        activeUser.setActive(false);
        when(jwtService.parseToken("validtoken")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void validToken_setsSecurityContextAndContinues() throws Exception {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer goodtoken");
        var res = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        var claims = mock(Claims.class);

        when(jwtService.parseToken("goodtoken")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(req, res);
    }
}
