package com.ffxiv.planner.common.web;

import com.ffxiv.planner.common.exception.EmailAlreadyExistsException;
import com.ffxiv.planner.common.exception.ForbiddenException;
import com.ffxiv.planner.common.exception.InvalidCredentialsException;
import com.ffxiv.planner.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ffxiv.planner.auth.jwt.JwtAuthenticationFilter;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import com.ffxiv.planner.auth.security.JwtAuthenticationEntryPoint;
import com.ffxiv.planner.auth.security.SecurityConfig;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa que o GlobalExceptionHandler mapeia cada exceção para o status HTTP
 * e formato RFC 7807 corretos, sem vazar stack traces.
 */
@WebMvcTest(GlobalExceptionHandlerTest.ExceptionTriggerController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        GlobalExceptionHandlerTest.JwtFixtures.class,
        GlobalExceptionHandlerTest.ExceptionTriggerController.class})
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.generateAccessToken(UUID.randomUUID());
    }

    // ── handlers já existentes ────────────────────────────────

    @Test
    void notFound_returns404WithProblemDetail() throws Exception {
        mockMvc.perform(get("/test-exceptions/not-found")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void forbidden_returns403WithProblemDetail() throws Exception {
        mockMvc.perform(get("/test-exceptions/forbidden")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void unauthorized_returns401WithProblemDetail() throws Exception {
        mockMvc.perform(get("/test-exceptions/unauthorized")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void conflict_returns409WithProblemDetail() throws Exception {
        mockMvc.perform(get("/test-exceptions/conflict")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409));
    }

    // ── handlers novos em AUTH-13 ─────────────────────────────

    @Test
    void illegalState_returns409WithProblemDetail() throws Exception {
        mockMvc.perform(get("/test-exceptions/illegal-state")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void unhandled_returns500WithoutStackTrace() throws Exception {
        mockMvc.perform(get("/test-exceptions/generic")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ── controller auxiliar de teste ──────────────────────────

    @RestController
    @RequestMapping("/test-exceptions")
    public static class ExceptionTriggerController {

        @GetMapping("/not-found")
        void throwNotFound() { throw new ResourceNotFoundException("Resource", UUID.randomUUID()); }

        @GetMapping("/forbidden")
        void throwForbidden() { throw new ForbiddenException(); }

        @GetMapping("/unauthorized")
        void throwUnauthorized() { throw new InvalidCredentialsException(); }

        @GetMapping("/conflict")
        void throwConflict() { throw new EmailAlreadyExistsException("user@test.com"); }

        @GetMapping("/illegal-state")
        void throwIllegalState() { throw new IllegalStateException("Estado inválido"); }

        @GetMapping("/generic")
        void throwGeneric() { throw new RuntimeException("Erro inesperado"); }
    }

    @TestConfiguration
    static class JwtFixtures {
        @Bean
        JwtTokenProvider jwtTokenProvider() {
            JwtProperties props = new JwtProperties();
            props.setSecret("test-jwt-secret-key-minimum-32-characters-long-for-hs256");
            props.setAccessExpiration(java.time.Duration.ofMinutes(15));
            props.setRefreshExpiration(java.time.Duration.ofDays(30));
            return new JwtTokenProvider(props);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider) {
            return new JwtAuthenticationFilter(provider);
        }
    }
}
