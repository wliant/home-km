package com.homekm.auth;

import com.homekm.common.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    // Base64 of 32 zero bytes — valid HMAC-SHA256 key, test only
    private static final String TEST_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(TEST_SECRET);
        props.getJwt().setExpiryHours(1);
        jwtService = new JwtService(props);

        testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", 42L);
        testUser.setEmail("alice@example.com");
        testUser.setAdmin(false);
        testUser.setChild(false);
    }

    @Test
    void generateToken_containsExpectedClaims() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("alice@example.com");
        assertThat(claims.get("isAdmin", Boolean.class)).isFalse();
        assertThat(claims.get("isChild", Boolean.class)).isFalse();
    }

    @Test
    void generateToken_adminClaimIsTrue_whenUserIsAdmin() {
        testUser.setAdmin(true);
        String token = jwtService.generateToken(testUser);

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.get("isAdmin", Boolean.class)).isTrue();
    }

    @Test
    void generateToken_childClaimIsTrue_whenUserIsChild() {
        testUser.setChild(true);
        String token = jwtService.generateToken(testUser);

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.get("isChild", Boolean.class)).isTrue();
    }

    @Test
    void parseToken_throwsForTamperedToken() {
        String token = jwtService.generateToken(testUser);
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "tampered";

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(Exception.class);
    }

    @Test
    void isExpired_returnsFalse_forFreshToken() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.isExpired(token)).isFalse();
    }

    @Test
    void getExpiry_returnsInstantInFuture() {
        Instant before = Instant.now();
        Instant expiry = jwtService.getExpiry(testUser);
        assertThat(expiry).isAfter(before);
    }

    @Test
    void twoTokens_forSameUser_haveDistinctIssuedAt() throws InterruptedException {
        String t1 = jwtService.generateToken(testUser);
        Thread.sleep(5);
        String t2 = jwtService.generateToken(testUser);
        assertThat(t1).isNotEqualTo(t2);
    }
}
