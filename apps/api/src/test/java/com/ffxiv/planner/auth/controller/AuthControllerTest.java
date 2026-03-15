package com.ffxiv.planner.auth.controller;

import com.ffxiv.planner.auth.dto.AuthResponse;
import com.ffxiv.planner.auth.jwt.JwtAuthenticationFilter;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import com.ffxiv.planner.auth.security.JwtAuthenticationEntryPoint;
import com.ffxiv.planner.auth.security.SecurityConfig;
import com.ffxiv.planner.auth.service.AuthService;
import com.ffxiv.planner.common.exception.EmailAlreadyExistsException;
import com.ffxiv.planner.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        AuthControllerTest.JwtFixtures.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockBean AuthService authService;

    private static final AuthResponse TOKENS =
            new AuthResponse("access-token", "refresh-token");

    // ── POST /auth/register ───────────────────────────────────

    @Test
    void register_returns201WithTokens() throws Exception {
        when(authService.register(any())).thenReturn(TOKENS);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@test.com","password":"senha123","locale":"pt-BR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void register_returns409WhenEmailExists() throws Exception {
        when(authService.register(any()))
                .thenThrow(new EmailAlreadyExistsException("user@test.com"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@test.com","password":"senha123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns422WhenEmailIsBlank() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":"senha123"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void register_returns422WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@test.com","password":"123"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void register_returns422WhenEmailInvalid() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"senha123"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /auth/login ──────────────────────────────────────

    @Test
    void login_returns200WithTokens() throws Exception {
        when(authService.login(any())).thenReturn(TOKENS);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@test.com","password":"senha123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_returns401ForInvalidCredentials() throws Exception {
        when(authService.login(any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@test.com","password":"errada"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns422WhenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /auth/refresh ────────────────────────────────────

    @Test
    void refresh_returns200WithNewTokens() throws Exception {
        when(authService.refresh(any())).thenReturn(TOKENS);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"old-refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void refresh_returns401ForInvalidToken() throws Exception {
        when(authService.refresh(any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"bad-token"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /auth/logout ─────────────────────────────────────

    @Test
    void logout_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(authService).logout(eq(userId));
    }

    @Test
    void logout_returns401WithoutToken() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).logout(any());
    }

    // ── POST /auth/forgot-password ────────────────────────────

    @Test
    void forgotPassword_returns202() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@test.com"}
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    void forgotPassword_returns422WhenEmailMissing() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── POST /auth/reset-password ─────────────────────────────

    @Test
    void resetPassword_returns204() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"reset-token","newPassword":"novaSenha123"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void resetPassword_returns422WhenTokenMissing() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"novaSenha123"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── fixtures ──────────────────────────────────────────────

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
