package com.ffxiv.planner.auth.oauth2;

import com.ffxiv.planner.auth.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Intercepta o sucesso do fluxo OAuth2 e redireciona o browser do usuário
 * de volta ao frontend com os tokens JWT como query params.
 *
 * <p>Mapeamento de atributos por provider:
 * <ul>
 *   <li><b>google</b>  → {@code sub} (subject identifier estável)</li>
 *   <li><b>discord</b> → {@code id}  (snowflake do usuário)</li>
 * </ul>
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthService oAuthService;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(OAuthService oAuthService,
                                     @Value("${app.frontend-url}") String frontendUrl) {
        this.oAuthService = oAuthService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        String providerId = resolveProviderId(provider, oAuth2User);
        String email = oAuth2User.getAttribute("email");

        if (email == null) {
            // Alguns providers (Discord sem e-mail verificado) podem não fornecer email
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "E-mail não disponível para o provider: " + provider);
            return;
        }

        AuthResponse authResponse = oAuthService.processOAuthLogin(provider, providerId, email);

        String redirectUrl = UriComponentsBuilder.fromHttpUrl(frontendUrl)
                .path("/auth/callback")
                .queryParam("accessToken", authResponse.accessToken())
                .queryParam("refreshToken", authResponse.refreshToken())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    // ── helpers ───────────────────────────────────────────────

    private String resolveProviderId(String provider, OAuth2User user) {
        return switch (provider) {
            case "google" -> user.getAttribute("sub");
            case "discord" -> user.getAttribute("id");
            default -> user.getName();
        };
    }
}
