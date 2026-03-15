package com.ffxiv.planner.auth.security;

import com.ffxiv.planner.auth.jwt.JwtAuthenticationFilter;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SecurityTestController.class, SecurityTestPublicController.class})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, SecurityConfigTest.Fixtures.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ── Rotas públicas de auth → 200 ────────────────────────

    @Test
    void postAuthRegister_isPublic() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void postAuthLogin_isPublic() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void postAuthRefresh_isPublic() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isOk());
    }

    @Test
    void postAuthForgotPassword_isPublic() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void postAuthResetPassword_isPublic() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ── GET em rota de auth é bloqueado (apenas POST é público) ──

    @Test
    void getAuthLogin_isBlocked() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isUnauthorized());
    }

    // ── Rota protegida sem token → 401 ───────────────────────

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized());
    }

    // ── 401 retorna application/problem+json ─────────────────

    @Test
    void unauthorized_returnsProblemJsonContentType() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/problem+json"));
    }

    // ── Rota protegida com token válido → 200 ────────────────

    @Test
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID());

        mockMvc.perform(get("/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── Rota protegida com token malformado → 401 ────────────

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/protected")
                        .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    // ── Rota protegida com token expirado → 401 ──────────────

    @Test
    void protectedEndpoint_withExpiredToken_returns401() throws Exception {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("test-jwt-secret-key-minimum-32-characters-long-for-hs256");
        shortProps.setAccessExpiration(Duration.ofMillis(1));
        shortProps.setRefreshExpiration(Duration.ofDays(30));
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortProps);

        String expired = shortProvider.generateAccessToken(UUID.randomUUID());
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        mockMvc.perform(get("/protected")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    // ── Authorization sem prefixo Bearer → 401 ───────────────

    @Test
    void protectedEndpoint_withoutBearerPrefix_returns401() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID());

        mockMvc.perform(get("/protected")
                        .header("Authorization", token)) // sem "Bearer "
                .andExpect(status().isUnauthorized());
    }

    // ── Fixtures compartilhadas ───────────────────────────────

    @TestConfiguration
    static class Fixtures {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            JwtProperties props = new JwtProperties();
            props.setSecret("test-jwt-secret-key-minimum-32-characters-long-for-hs256");
            props.setAccessExpiration(Duration.ofMinutes(15));
            props.setRefreshExpiration(Duration.ofDays(30));
            return new JwtTokenProvider(props);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider) {
            return new JwtAuthenticationFilter(provider);
        }
    }
}
