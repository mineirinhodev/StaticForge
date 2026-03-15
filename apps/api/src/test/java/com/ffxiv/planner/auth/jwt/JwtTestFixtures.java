package com.ffxiv.planner.auth.jwt;

import java.time.Duration;

/**
 * Shared fixture factory for JWT unit tests.
 * Keeps the test secret and default expiration in a single place.
 */
final class JwtTestFixtures {

    static final String TEST_SECRET =
            "test-jwt-secret-key-minimum-32-characters-long-for-hs256";

    private JwtTestFixtures() {}

    static JwtTokenProvider defaultProvider() {
        JwtProperties props = new JwtProperties();
        props.setSecret(TEST_SECRET);
        props.setAccessExpiration(Duration.ofMinutes(15));
        props.setRefreshExpiration(Duration.ofDays(30));
        return new JwtTokenProvider(props);
    }
}
