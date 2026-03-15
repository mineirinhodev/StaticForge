package com.ffxiv.planner;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FfxivPlannerApplicationTests {

    // Redis não está disponível no perfil test (sem Docker necessário).
    // Mockar as factories evita falha no startup do contexto completo.
    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    @MockBean
    ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Test
    void contextLoads() {
    }
}
