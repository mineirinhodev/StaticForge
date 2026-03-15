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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setLocale(request.locale());
        user.setActive(true);

        user = userRepository.save(user);
        return issueTokens(user, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);
            throw new InvalidCredentialsException();
        }

        user.setFailedLoginAttempts(0);
        return issueTokens(user, null);
    }

    @Transactional
    public AuthResponse refresh(String rawToken) {
        String hash = hashToken(rawToken);

        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(InvalidCredentialsException::new);

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException();
        }

        if (!stored.getUser().isActive()) {
            throw new InvalidCredentialsException();
        }

        // Emitir novo par antes de revogar o antigo, para obter o hash do substituto
        AuthResponse response = issueTokens(stored.getUser(), stored);

        // Registrar qual token substituiu este (reuse detection)
        stored.setRevokedAt(Instant.now());

        return response;
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ── helpers ───────────────────────────────────────────────

    /**
     * Emite um par access + refresh token para o usuário.
     *
     * @param predecessor token que está sendo rotacionado, ou null (primeiro login/registro).
     *                    Quando informado, seu campo {@code replacedBy} é preenchido com o hash
     *                    do novo token para habilitar reuse detection.
     */
    private AuthResponse issueTokens(User user, RefreshToken predecessor) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String rawRefresh = jwtTokenProvider.generateRefreshToken();
        String newHash = hashToken(rawRefresh);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(newHash);
        rt.setCreatedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshExpiration()));
        refreshTokenRepository.save(rt);

        if (predecessor != null) {
            predecessor.setReplacedBy(newHash);
        }

        return new AuthResponse(accessToken, rawRefresh);
    }

    /**
     * Gera hash SHA-256 do token em hexadecimal (64 chars).
     * O valor plano nunca é persistido no banco.
     */
    public static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}
