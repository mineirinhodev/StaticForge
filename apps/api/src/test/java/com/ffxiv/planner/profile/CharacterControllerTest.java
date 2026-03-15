package com.ffxiv.planner.profile;

import com.ffxiv.planner.auth.jwt.JwtAuthenticationFilter;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import com.ffxiv.planner.auth.security.JwtAuthenticationEntryPoint;
import com.ffxiv.planner.auth.security.SecurityConfig;
import com.ffxiv.planner.profile.controller.CharacterController;
import com.ffxiv.planner.profile.dto.CharacterCreateRequest;
import com.ffxiv.planner.profile.dto.CharacterResponse;
import com.ffxiv.planner.profile.service.CharacterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CharacterController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        CharacterControllerTest.JwtFixtures.class})
class CharacterControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockBean CharacterService characterService;

    // ── GET /characters ───────────────────────────────────────

    @Test
    void listCharacters_returns200WithList() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);
        when(characterService.listCharacters(userId)).thenReturn(List.of(characterResponse(true)));

        mockMvc.perform(get("/characters")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isMain").value(true));
    }

    @Test
    void listCharacters_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/characters"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /characters ──────────────────────────────────────

    @Test
    void createCharacter_returns201WithCreatedCharacter() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);
        when(characterService.createCharacter(eq(userId), any(CharacterCreateRequest.class)))
                .thenReturn(characterResponse(true));

        mockMvc.perform(post("/characters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Luna","server":"Behemoth","dataCenter":"Primal","isMain":false,"lookingForStatic":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Luna"));
    }

    @Test
    void createCharacter_returns422WhenNameMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        mockMvc.perform(post("/characters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"server":"Behemoth","dataCenter":"Primal"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── PUT /characters/{id}/main ─────────────────────────────

    @Test
    void setMainCharacter_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID charId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);
        when(characterService.setMainCharacter(userId, charId)).thenReturn(characterResponse(true));

        mockMvc.perform(put("/characters/{id}/main", charId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMain").value(true));

        verify(characterService).setMainCharacter(userId, charId);
    }

    // ── DELETE /characters/{id} ───────────────────────────────

    @Test
    void deleteCharacter_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID charId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        mockMvc.perform(delete("/characters/{id}", charId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(characterService).deleteCharacter(userId, charId);
    }

    // ── helpers / fixtures ────────────────────────────────────

    private CharacterResponse characterResponse(boolean isMain) {
        return new CharacterResponse(UUID.randomUUID(), "Luna", "Behemoth", "Primal",
                null, false, isMain, null, null, null, null, null,
                false, null, List.of());
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
