package com.ffxiv.planner.auth.oauth2;

import com.ffxiv.planner.auth.dto.AuthResponse;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import com.ffxiv.planner.auth.repository.RefreshTokenRepository;
import com.ffxiv.planner.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-jwt-secret-key-minimum-32-characters-long-for-hs256");
        props.setAccessExpiration(Duration.ofMinutes(15));
        props.setRefreshExpiration(Duration.ofDays(30));

        oAuthService = new OAuthService(userRepository, refreshTokenRepository,
                jwtTokenProvider, props);
    }

    // ── novo usuário via OAuth ────────────────────────────────

    @Test
    void processOAuthLogin_createsNewUserWhenNotFound() {
        when(userRepository.findByEmail("new@google.com")).thenReturn(Optional.empty());
        when(userRepository.findByOAuthProvider("google", "g-123")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(UUID.randomUUID().toString());

        AuthResponse response = oAuthService.processOAuthLogin("google", "g-123", "new@google.com");

        assertThat(response.accessToken()).isEqualTo("access");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@google.com");
        assertThat(saved.getOauthProvider()).isEqualTo("google");
        assertThat(saved.getOauthProviderId()).isEqualTo("g-123");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getPasswordHash()).isNull(); // sem senha para OAuth
    }

    // ── usuário existente via providerId ─────────────────────

    @Test
    void processOAuthLogin_returnsTokensForExistingOAuthUser() {
        User existing = existingUser("user@google.com", "google", "g-123");
        when(userRepository.findByOAuthProvider("google", "g-123"))
                .thenReturn(Optional.of(existing));
        when(jwtTokenProvider.generateAccessToken(existing.getId())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(UUID.randomUUID().toString());

        AuthResponse response = oAuthService.processOAuthLogin("google", "g-123", "user@google.com");

        assertThat(response.accessToken()).isEqualTo("access");
        verify(userRepository, never()).save(any()); // nenhum save necessário
    }

    // ── vincular conta OAuth a usuário existente por email ───

    @Test
    void processOAuthLogin_linksOAuthToExistingEmailAccount() {
        // usuário cadastrou-se com email+senha, agora loga com Google
        User existing = existingUser("user@example.com", null, null);
        when(userRepository.findByOAuthProvider("google", "g-456"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenReturn(existing);
        when(jwtTokenProvider.generateAccessToken(existing.getId())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(UUID.randomUUID().toString());

        oAuthService.processOAuthLogin("google", "g-456", "user@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getOauthProvider()).isEqualTo("google");
        assertThat(captor.getValue().getOauthProviderId()).isEqualTo("g-456");
    }

    // ── Discord ───────────────────────────────────────────────

    @Test
    void processOAuthLogin_worksForDiscord() {
        when(userRepository.findByOAuthProvider("discord", "d-789")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@discord.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(UUID.randomUUID().toString());

        AuthResponse response = oAuthService.processOAuthLogin("discord", "d-789", "user@discord.com");

        assertThat(response.accessToken()).isNotBlank();
        verify(userRepository).save(argThat(u ->
                "discord".equals(u.getOauthProvider()) && "d-789".equals(u.getOauthProviderId())));
    }

    // ── refresh token é armazenado como hash ─────────────────

    @Test
    void processOAuthLogin_storesRefreshTokenHash() {
        User existing = existingUser("user@google.com", "google", "g-123");
        String rawToken = UUID.randomUUID().toString();

        when(userRepository.findByOAuthProvider("google", "g-123"))
                .thenReturn(Optional.of(existing));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(rawToken);

        oAuthService.processOAuthLogin("google", "g-123", "user@google.com");

        var captor = ArgumentCaptor.forClass(com.ffxiv.planner.auth.entity.RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawToken);
        assertThat(captor.getValue().getTokenHash()).hasSize(64);
    }

    // ── helper ────────────────────────────────────────────────

    private User existingUser(String email, String provider, String providerId) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setOauthProvider(provider);
        u.setOauthProviderId(providerId);
        u.setActive(true);
        return u;
    }
}
