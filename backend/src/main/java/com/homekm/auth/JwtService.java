package com.homekm.auth;

import com.homekm.common.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiryHours;

    public JwtService(AppProperties props) {
        byte[] keyBytes = Base64.getDecoder().decode(props.getJwt().getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiryHours = props.getJwt().getExpiryHours();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expiryHours, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("isAdmin", user.isAdmin())
                .claim("isChild", user.isChild())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Instant getExpiry(User user) {
        return Instant.now().plus(expiryHours, ChronoUnit.HOURS);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isExpired(String token) {
        try {
            parseToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
