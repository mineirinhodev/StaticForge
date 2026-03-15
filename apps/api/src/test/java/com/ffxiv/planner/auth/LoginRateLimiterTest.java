package com.ffxiv.planner.auth;

import com.ffxiv.planner.auth.service.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // opsForValue() só é necessário em testes que lêem/incrementam contadores
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        rateLimiter = new LoginRateLimiter(redisTemplate);
    }

    // ── isBlocked ─────────────────────────────────────────────

    @Test
    void isBlocked_returnsFalseWhenBelowLimit() {
        when(valueOps.get(anyString())).thenReturn("3");

        assertThat(rateLimiter.isBlocked("127.0.0.1", "user@test.com")).isFalse();
    }

    @Test
    void isBlocked_returnsTrueWhenIpAtOrOverLimit() {
        when(valueOps.get(startsWith("login:ip:"))).thenReturn("5");
        when(valueOps.get(startsWith("login:email:"))).thenReturn("1");

        assertThat(rateLimiter.isBlocked("127.0.0.1", "user@test.com")).isTrue();
    }

    @Test
    void isBlocked_returnsTrueWhenEmailAtOrOverLimit() {
        when(valueOps.get(startsWith("login:ip:"))).thenReturn("1");
        when(valueOps.get(startsWith("login:email:"))).thenReturn("10");

        assertThat(rateLimiter.isBlocked("127.0.0.1", "user@test.com")).isTrue();
    }

    @Test
    void isBlocked_returnsFalseWhenCounterIsNull() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertThat(rateLimiter.isBlocked("127.0.0.1", "user@test.com")).isFalse();
    }

    // ── recordFailedAttempt ───────────────────────────────────

    @Test
    void recordFailedAttempt_incrementsCountersAndSetsTtl() {
        when(valueOps.increment(anyString())).thenReturn(1L);

        rateLimiter.recordFailedAttempt("127.0.0.1", "user@test.com");

        verify(valueOps).increment(contains("login:ip:127.0.0.1"));
        verify(valueOps).increment(contains("login:email:user@test.com"));
        verify(redisTemplate, times(2)).expire(anyString(), any());
    }

    // ── clearAttempts ─────────────────────────────────────────

    @Test
    void clearAttempts_deletesKeysForIpAndEmail() {
        rateLimiter.clearAttempts("127.0.0.1", "user@test.com");

        verify(redisTemplate).delete(contains("login:ip:127.0.0.1"));
        verify(redisTemplate).delete(contains("login:email:user@test.com"));
    }
}
