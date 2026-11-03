package com.sirius.sirius.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-ttl}") Duration accessTokenTtl
    ) {
        byte[] decodedSecret;

        try {
            decodedSecret = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT secret must be a valid Base64 string",
                    ex
            );
        }

        if (decodedSecret.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must contain at least 32 bytes (256 bits)"
            );
        }

        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalStateException(
                    "JWT access token TTL must be greater than zero"
            );
        }

        this.key = Keys.hmacShaKeyFor(decodedSecret);
        this.accessTokenTtl = accessTokenTtl;
    }

    public String generateToken(String email) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }
}