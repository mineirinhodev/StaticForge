package com.ffxiv.planner.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET =
            "test-jwt-secret-key-minimum-32-characters-long-for-hs256";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessExpiration(Duration.ofMinutes(15));
        props.setRefreshExpiration(Duration.ofDays(30));
        provider = new JwtTokenProvider(props);
    }

    // ── generateAccessToken ───────────────────────────────────

    @Test
    void generateAccessToken_returnsNonBlankToken() {
        UUID userId = UUID.randomUUID();

        String token = provider.generateAccessToken(userId);

        assertThat(token).isNotBlank();
    }

    @Test
    void generateAccessToken_producesUniqueTokensForSameUser() {
        UUID userId = UUID.randomUUID();

        String token1 = provider.generateAccessToken(userId);
        String token2 = provider.generateAccessToken(userId);

        assertThat(token1).isNotEqualTo(token2);
    }

    // ── getUserIdFromToken ────────────────────────────────────

    @Test
    void getUserIdFromToken_returnsCorrectUserId() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId);

        UUID extracted = provider.getUserIdFromToken(token);

        assertThat(extracted).isEqualTo(userId);
    }

    // ── validateToken ─────────────────────────────────────────

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = provider.generateAccessToken(UUID.randomUUID());

        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalseForExpiredToken() {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret(SECRET);
        shortProps.setAccessExpiration(Duration.ofMillis(1));
        shortProps.setRefreshExpiration(Duration.ofDays(30));
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortProps);

        String token = shortProvider.generateAccessToken(UUID.randomUUID());
        // Ensure token is expired
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(shortProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_returnsFalseForTamperedToken() {
        String token = provider.generateAccessToken(UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_returnsFalseForBlankToken() {
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.validateToken(null)).isFalse();
    }

    // ── getExpirationFromToken ────────────────────────────────

    @Test
    void getExpirationFromToken_returnsFutureInstant() {
        String token = provider.generateAccessToken(UUID.randomUUID());

        Instant expiration = provider.getExpirationFromToken(token);

        assertThat(expiration).isAfter(Instant.now());
    }

    @Test
    void getExpirationFromToken_matchesConfiguredExpiration() {
        String token = provider.generateAccessToken(UUID.randomUUID());
        Instant issuedAt = Instant.now();

        Instant expiration = provider.getExpirationFromToken(token);

        // Should be approximately 15 minutes from now (within 5 seconds tolerance)
        assertThat(expiration).isBetween(
                issuedAt.plus(Duration.ofMinutes(14).plusSeconds(55)),
                issuedAt.plus(Duration.ofMinutes(15).plusSeconds(5))
        );
    }

    // ── generateRefreshToken ──────────────────────────────────

    @Test
    void generateRefreshToken_returnsNonBlankString() {
        String token = provider.generateRefreshToken();

        assertThat(token).isNotBlank();
    }

    @Test
    void generateRefreshToken_producesUniqueValues() {
        String token1 = provider.generateRefreshToken();
        String token2 = provider.generateRefreshToken();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void generateRefreshToken_isNotAJwt() {
        String token = provider.generateRefreshToken();

        // JWTs have exactly 2 dots (header.payload.signature)
        assertThat(token.split("\\.")).hasSize(1);
    }
}
