package com.ffxiv.planner.profile;

import com.ffxiv.planner.auth.jwt.JwtAuthenticationFilter;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import com.ffxiv.planner.auth.security.JwtAuthenticationEntryPoint;
import com.ffxiv.planner.auth.security.SecurityConfig;
import com.ffxiv.planner.profile.controller.ProfileController;
import com.ffxiv.planner.profile.dto.ProfileResponse;
import com.ffxiv.planner.profile.dto.ProfileUpdateRequest;
import com.ffxiv.planner.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        ProfileControllerTest.JwtFixtures.class})
class ProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockBean ProfileService profileService;

    // ── GET /profile/me ───────────────────────────────────────

    @Test
    void getMyProfile_returns200WithProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);
        ProfileResponse profile = profileResponse(userId, "user@test.com", "pt-BR");

        when(profileService.getMyProfile(userId)).thenReturn(profile);

        mockMvc.perform(get("/profile/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.locale").value("pt-BR"));
    }

    @Test
    void getMyProfile_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/profile/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /profile/me ───────────────────────────────────────

    @Test
    void updateProfile_returns200WithUpdatedProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);
        ProfileResponse updated = profileResponse(userId, "user@test.com", "en");

        when(profileService.updateProfile(eq(userId), any(ProfileUpdateRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locale":"en","bio":"Nova bio"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale").value("en"));

        verify(profileService).updateProfile(eq(userId), any());
    }

    @Test
    void updateProfile_returns401WithoutToken() throws Exception {
        mockMvc.perform(put("/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locale":"en"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /users/{id}/profile ────────────────────────────────

    @Test
    void getPublicProfile_returns200WithoutEmail() throws Exception {
        UUID targetId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(callerId);
        ProfileResponse pub = profileResponse(targetId, null, "en");

        when(profileService.getPublicProfile(targetId)).thenReturn(pub);

        mockMvc.perform(get("/users/{id}/profile", targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    // ── helpers / fixtures ────────────────────────────────────

    private ProfileResponse profileResponse(UUID id, String email, String locale) {
        return new ProfileResponse(id, email, locale, false, null, List.of(), Instant.now(), Instant.now());
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
