package com.ffxiv.planner.auth.security;

import com.ffxiv.planner.auth.jwt.JwtAuthenticationFilter;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityTestController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, SecurityConfigTest.Fixtures.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ── Rotas públicas de auth ────────────────────────────────

    @Test
    void postAuthRegister_isPublic() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound()); // 404 (sem controller real), não 401/403
    }

    @Test
    void postAuthLogin_isPublic() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postAuthRefresh_isPublic() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postAuthForgotPassword_isPublic() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postAuthResetPassword_isPublic() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // ── Rota protegida sem token → 401 ───────────────────────

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized());
    }

    // ── Rota protegida com token válido → 200 ────────────────

    @Test
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID());

        mockMvc.perform(get("/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── Rota protegida com token inválido → 401 ──────────────

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/protected")
                        .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    // ── 401 retorna JSON (não redirect HTML) ─────────────────

    @Test
    void unauthorized_returnsJsonContentType() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertThat(result.getResponse().getContentType())
                                .contains("application/json"));
    }

    // ── PasswordEncoder está disponível como bean ─────────────

    @Test
    void passwordEncoder_isAvailable() {
        assertThat(passwordEncoder).isNotNull();
        String hash = passwordEncoder.encode("senha123");
        assertThat(passwordEncoder.matches("senha123", hash)).isTrue();
        assertThat(passwordEncoder.matches("errada", hash)).isFalse();
    }

    // ── Fixtures compartilhadas ───────────────────────────────

    @TestConfiguration
    static class Fixtures {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            com.ffxiv.planner.auth.jwt.JwtProperties props =
                    new com.ffxiv.planner.auth.jwt.JwtProperties();
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
