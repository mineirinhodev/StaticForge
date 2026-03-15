package com.ffxiv.planner.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = JwtTestFixtures.defaultProvider();
    }

    // ── generateAccessToken ───────────────────────────────────

    @Test
    void generateAccessToken_returnsNonBlankToken() {
        String token = provider.generateAccessToken(UUID.randomUUID());

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

        assertThat(provider.getUserIdFromToken(token)).isEqualTo(userId);
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
        shortProps.setSecret(JwtTestFixtures.TEST_SECRET);
        shortProps.setAccessExpiration(Duration.ofMillis(1));
        shortProps.setRefreshExpiration(Duration.ofDays(30));
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortProps);

        String token = shortProvider.generateAccessToken(UUID.randomUUID());
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

        assertThat(provider.getExpirationFromToken(token)).isAfter(Instant.now());
    }

    @Test
    void getExpirationFromToken_matchesConfiguredExpiration() {
        String token = provider.generateAccessToken(UUID.randomUUID());
        Instant issuedAt = Instant.now();

        Instant expiration = provider.getExpirationFromToken(token);

        assertThat(expiration).isBetween(
                issuedAt.plus(Duration.ofMinutes(14).plusSeconds(55)),
                issuedAt.plus(Duration.ofMinutes(15).plusSeconds(5))
        );
    }

    // ── generateRefreshToken ──────────────────────────────────

    @Test
    void generateRefreshToken_returnsNonBlankString() {
        assertThat(provider.generateRefreshToken()).isNotBlank();
    }

    @Test
    void generateRefreshToken_producesUniqueValues() {
        assertThat(provider.generateRefreshToken()).isNotEqualTo(provider.generateRefreshToken());
    }

    @Test
    void generateRefreshToken_isValidUuid() {
        String token = provider.generateRefreshToken();

        // Asserts that the token is a valid UUID (not a JWT)
        assertThatCode(() -> UUID.fromString(token)).doesNotThrowAnyException();
    }
}
