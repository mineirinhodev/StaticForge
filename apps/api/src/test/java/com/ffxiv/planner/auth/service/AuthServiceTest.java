package com.ffxiv.planner.auth.service;

import com.ffxiv.planner.auth.dto.AuthResponse;
import com.ffxiv.planner.auth.dto.LoginRequest;
import com.ffxiv.planner.auth.dto.RegisterRequest;
import com.ffxiv.planner.auth.entity.RefreshToken;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import com.ffxiv.planner.auth.repository.RefreshTokenRepository;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.common.exception.EmailAlreadyExistsException;
import com.ffxiv.planner.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-jwt-secret-key-minimum-32-characters-long-for-hs256");
        props.setAccessExpiration(Duration.ofMinutes(15));
        props.setRefreshExpiration(Duration.ofDays(30));

        authService = new AuthService(
                userRepository, refreshTokenRepository,
                passwordEncoder, jwtTokenProvider, props);
    }

    // ── register ─────────────────────────────────────────────

    @Test
    void register_savesNewUser() {
        var req = new RegisterRequest("user@example.com", "senha123", "pt-BR");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(UUID.randomUUID().toString());

        AuthResponse response = authService.register(req);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isNotBlank();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        var req = new RegisterRequest("dup@example.com", "senha123", "pt-BR");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_storesRefreshTokenHash_notPlainValue() {
        var req = new RegisterRequest("user@example.com", "senha123", "pt-BR");
        String rawToken = UUID.randomUUID().toString();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(rawToken);

        authService.register(req);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        // hash SHA-256 deve ser armazenado, nunca o valor raw
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawToken);
        assertThat(captor.getValue().getTokenHash()).hasSize(64); // hex SHA-256
    }

    // ── login ────────────────────────────────────────────────

    @Test
    void login_returnsTokensForValidCredentials() {
        var req = new LoginRequest("user@example.com", "senha123");
        User user = activeUser("user@example.com", "hash");

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), "hash")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user.getId())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(UUID.randomUUID().toString());

        AuthResponse response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void login_throwsForUnknownEmail() {
        var req = new LoginRequest("nobody@example.com", "senha123");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsForWrongPassword() {
        var req = new LoginRequest("user@example.com", "errada");
        User user = activeUser("user@example.com", "hash");

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsForInactiveUser() {
        var req = new LoginRequest("user@example.com", "senha123");
        User user = activeUser("user@example.com", "hash");
        user.setActive(false);

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);

        // senha nem deve ser verificada para usuário inativo
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_resetsFailedAttemptsOnSuccess() {
        var req = new LoginRequest("user@example.com", "senha123");
        User user = activeUser("user@example.com", "hash");
        user.setFailedLoginAttempts(3);

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(UUID.randomUUID().toString());

        authService.login(req);

        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void login_incrementsFailedAttemptsOnWrongPassword() {
        var req = new LoginRequest("user@example.com", "errada");
        User user = activeUser("user@example.com", "hash");

        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    // ── refresh ──────────────────────────────────────────────

    @Test
    void refresh_returnsNewTokensForValidRefreshToken() {
        String rawToken = UUID.randomUUID().toString();
        String hash = AuthService.hashToken(rawToken);

        User user = activeUser("user@example.com", "hash");
        RefreshToken stored = validRefreshToken(user, hash);

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash))
                .thenReturn(Optional.of(stored));
        when(jwtTokenProvider.generateAccessToken(user.getId())).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(UUID.randomUUID().toString());

        AuthResponse response = authService.refresh(rawToken);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void refresh_revokesOldTokenAfterRotation() {
        String rawToken = UUID.randomUUID().toString();
        String hash = AuthService.hashToken(rawToken);

        User user = activeUser("user@example.com", "hash");
        RefreshToken stored = validRefreshToken(user, hash);

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash))
                .thenReturn(Optional.of(stored));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(UUID.randomUUID().toString());

        authService.refresh(rawToken);

        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void refresh_populatesReplacedByForReuseDetection() {
        String rawToken = UUID.randomUUID().toString();
        String hash = AuthService.hashToken(rawToken);

        User user = activeUser("user@example.com", "hash");
        RefreshToken stored = validRefreshToken(user, hash);

        String newRaw = UUID.randomUUID().toString();
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash))
                .thenReturn(Optional.of(stored));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(newRaw);

        authService.refresh(rawToken);

        // replacedBy deve conter o hash SHA-256 do novo token
        assertThat(stored.getReplacedBy()).isEqualTo(AuthService.hashToken(newRaw));
    }

    @Test
    void refresh_throwsForUnknownToken() {
        String rawToken = UUID.randomUUID().toString();
        String hash = AuthService.hashToken(rawToken);

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_throwsForExpiredToken() {
        String rawToken = UUID.randomUUID().toString();
        String hash = AuthService.hashToken(rawToken);

        User user = activeUser("user@example.com", "hash");
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setTokenHash(hash);
        stored.setExpiresAt(Instant.now().minusSeconds(1)); // expirado

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_throwsForInactiveUser() {
        String rawToken = UUID.randomUUID().toString();
        String hash = AuthService.hashToken(rawToken);

        User user = activeUser("user@example.com", "hash");
        user.setActive(false);
        RefreshToken stored = validRefreshToken(user, hash);

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(rawToken))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ── logout ───────────────────────────────────────────────

    @Test
    void logout_revokesAllUserRefreshTokens() {
        UUID userId = UUID.randomUUID();

        authService.logout(userId);

        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    // ── helpers ───────────────────────────────────────────────

    private User activeUser(String email, String hash) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setActive(true);
        return u;
    }

    private RefreshToken validRefreshToken(User user, String hash) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash);
        rt.setExpiresAt(Instant.now().plusSeconds(86400));
        return rt;
    }
}
