package com.ffxiv.planner.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final JwtParser jwtParser;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .build();
    }

    public String generateAccessToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessExpiration())))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a random refresh token (UUID, not JWT).
     * The caller is responsible for hashing (SHA-256) before storing in DB.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Parses and validates the token in a single pass.
     * Prefer this over validateToken + getUserIdFromToken to avoid double parsing.
     */
    public Optional<Claims> parseClaimsIfValid(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            return Optional.of(parseClaims(token));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public Instant getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public boolean validateToken(String token) {
        return parseClaimsIfValid(token).isPresent();
    }

    private Claims parseClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }
}
