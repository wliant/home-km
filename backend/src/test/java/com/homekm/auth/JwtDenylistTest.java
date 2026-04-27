package com.homekm.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class JwtDenylistTest {

    private JwtDenylist denylist;

    @BeforeEach
    void setUp() {
        denylist = new JwtDenylist();
    }

    @Test
    void revoke_addsToList() {
        String jti = "abc-123";
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        denylist.revoke(jti, expiresAt);

        assertThat(denylist.isRevoked(jti)).isTrue();
    }

    @Test
    void isRevoked_returnsFalseForUnknownJti() {
        assertThat(denylist.isRevoked("unknown-jti")).isFalse();
    }

    @Test
    void isRevoked_returnsFalseForExpiredJti() {
        String jti = "expired-jti";
        Instant pastExpiry = Instant.now().minus(1, ChronoUnit.HOURS);

        denylist.revoke(jti, pastExpiry);

        // The revoke method cleans up expired entries, and isRevoked also removes expired ones
        assertThat(denylist.isRevoked(jti)).isFalse();
    }

    @Test
    void size_reflectsActiveEntries() {
        assertThat(denylist.size()).isZero();

        denylist.revoke("jti-1", Instant.now().plus(1, ChronoUnit.HOURS));
        denylist.revoke("jti-2", Instant.now().plus(1, ChronoUnit.HOURS));

        assertThat(denylist.size()).isEqualTo(2);
    }
}
