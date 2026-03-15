package com.ffxiv.planner.auth.oauth2;

import com.ffxiv.planner.auth.dto.AuthResponse;
import com.ffxiv.planner.auth.entity.RefreshToken;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import com.ffxiv.planner.auth.repository.RefreshTokenRepository;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.auth.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class OAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public OAuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        JwtTokenProvider jwtTokenProvider,
                        JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Localiza ou cria um usuário com base no provider OAuth, e emite tokens JWT.
     *
     * <p>Resolução de conta (em ordem de prioridade):
     * <ol>
     *   <li>Usuário já vinculado ao mesmo provider+providerId → retorna tokens direto.</li>
     *   <li>Usuário com mesmo email (cadastro email+senha) → vincula OAuth e salva.</li>
     *   <li>Nenhum encontrado → cria novo usuário OAuth.</li>
     * </ol>
     */
    @Transactional
    public AuthResponse processOAuthLogin(String provider, String providerId, String email) {
        User user = userRepository.findByOAuthProvider(provider, providerId)
                .orElseGet(() -> resolveByEmailOrCreate(provider, providerId, email));

        return issueTokens(user);
    }

    // ── helpers ───────────────────────────────────────────────

    private User resolveByEmailOrCreate(String provider, String providerId, String email) {
        return userRepository.findByEmail(email)
                .map(existing -> linkOAuth(existing, provider, providerId))
                .orElseGet(() -> createOAuthUser(provider, providerId, email));
    }

    private User linkOAuth(User user, String provider, String providerId) {
        user.setOauthProvider(provider);
        user.setOauthProviderId(providerId);
        return userRepository.save(user);
    }

    private User createOAuthUser(String provider, String providerId, String email) {
        User user = new User();
        user.setEmail(email);
        user.setOauthProvider(provider);
        user.setOauthProviderId(providerId);
        user.setActive(true);
        // passwordHash fica null intencionalmente: usuário OAuth não tem senha local
        return userRepository.save(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String rawRefresh = jwtTokenProvider.generateRefreshToken();
        String hash = AuthService.hashToken(rawRefresh);

        Instant now = Instant.now();
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash);
        rt.setCreatedAt(now);
        rt.setExpiresAt(now.plus(jwtProperties.getRefreshExpiration()));
        refreshTokenRepository.save(rt);

        return new AuthResponse(accessToken, rawRefresh);
    }
}
