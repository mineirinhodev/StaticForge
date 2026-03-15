package com.ffxiv.planner.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Controla tentativas de login falhas via contadores Redis.
 *
 * <p>Limites:
 * <ul>
 *   <li>IP: 5 tentativas em 15 minutos</li>
 *   <li>Email: 10 tentativas em 1 hora</li>
 * </ul>
 */
@Component
public class LoginRateLimiter {

    private static final int IP_MAX_ATTEMPTS = 5;
    private static final int EMAIL_MAX_ATTEMPTS = 10;
    private static final Duration IP_WINDOW = Duration.ofMinutes(15);
    private static final Duration EMAIL_WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public LoginRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Verifica se o IP ou email atingiu o limite de tentativas falhas.
     */
    public boolean isBlocked(String ip, String email) {
        String ipCount = redisTemplate.opsForValue().get(ipKey(ip));
        String emailCount = redisTemplate.opsForValue().get(emailKey(email));

        return parse(ipCount) >= IP_MAX_ATTEMPTS || parse(emailCount) >= EMAIL_MAX_ATTEMPTS;
    }

    /**
     * Registra uma tentativa de login falhada e define/atualiza o TTL.
     */
    public void recordFailedAttempt(String ip, String email) {
        Long ipAttempts = redisTemplate.opsForValue().increment(ipKey(ip));
        if (ipAttempts != null && ipAttempts == 1) {
            redisTemplate.expire(ipKey(ip), IP_WINDOW);
        }

        Long emailAttempts = redisTemplate.opsForValue().increment(emailKey(email));
        if (emailAttempts != null && emailAttempts == 1) {
            redisTemplate.expire(emailKey(email), EMAIL_WINDOW);
        }
    }

    /**
     * Limpa os contadores após login bem-sucedido.
     */
    public void clearAttempts(String ip, String email) {
        redisTemplate.delete(ipKey(ip));
        redisTemplate.delete(emailKey(email));
    }

    // ── helpers ───────────────────────────────────────────────

    private String ipKey(String ip) {
        return "login:ip:" + ip;
    }

    private String emailKey(String email) {
        return "login:email:" + email;
    }

    private int parse(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
