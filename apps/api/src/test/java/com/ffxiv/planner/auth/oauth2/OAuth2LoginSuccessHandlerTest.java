package com.ffxiv.planner.auth.oauth2;

import com.ffxiv.planner.auth.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock private OAuthService oAuthService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginSuccessHandler(oAuthService, "http://localhost:3000");
    }

    // ── Google: extrai sub como providerId ───────────────────

    @Test
    void onAuthenticationSuccess_googleExtractsSubAndEmail() throws Exception {
        OAuth2User user = new DefaultOAuth2User(
                Set.of(),
                Map.of("sub", "g-001", "email", "google@test.com"),
                "sub");
        OAuth2AuthenticationToken token =
                new OAuth2AuthenticationToken(user, Set.of(), "google");

        when(oAuthService.processOAuthLogin(eq("google"), eq("g-001"), eq("google@test.com")))
                .thenReturn(new AuthResponse("access-jwt", "refresh-uuid"));

        handler.onAuthenticationSuccess(request, response, token);

        verify(oAuthService).processOAuthLogin("google", "g-001", "google@test.com");
        verify(response).sendRedirect(contains("accessToken=access-jwt"));
    }

    // ── Discord: extrai id como providerId ───────────────────

    @Test
    void onAuthenticationSuccess_discordExtractsIdAndEmail() throws Exception {
        OAuth2User user = new DefaultOAuth2User(
                Set.of(),
                Map.of("id", "d-002", "email", "discord@test.com"),
                "id");
        OAuth2AuthenticationToken token =
                new OAuth2AuthenticationToken(user, Set.of(), "discord");

        when(oAuthService.processOAuthLogin(eq("discord"), eq("d-002"), eq("discord@test.com")))
                .thenReturn(new AuthResponse("access-jwt", "refresh-uuid"));

        handler.onAuthenticationSuccess(request, response, token);

        verify(oAuthService).processOAuthLogin("discord", "d-002", "discord@test.com");
        verify(response).sendRedirect(contains("accessToken=access-jwt"));
    }

    // ── redirect inclui refreshToken ─────────────────────────

    @Test
    void onAuthenticationSuccess_redirectIncludesRefreshToken() throws Exception {
        OAuth2User user = new DefaultOAuth2User(
                Set.of(),
                Map.of("sub", "g-003", "email", "user@test.com"),
                "sub");
        OAuth2AuthenticationToken token =
                new OAuth2AuthenticationToken(user, Set.of(), "google");

        when(oAuthService.processOAuthLogin(any(), any(), any()))
                .thenReturn(new AuthResponse("acc", "ref"));

        handler.onAuthenticationSuccess(request, response, token);

        verify(response).sendRedirect(contains("refreshToken=ref"));
    }
}
