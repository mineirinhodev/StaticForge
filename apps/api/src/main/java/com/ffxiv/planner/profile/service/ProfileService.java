package com.ffxiv.planner.profile.service;

import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.RefreshTokenRepository;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.common.exception.ResourceNotFoundException;
import com.ffxiv.planner.profile.dto.ProfileResponse;
import com.ffxiv.planner.profile.dto.ProfileUpdateRequest;
import com.ffxiv.planner.profile.event.ProfileUpdatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProfileService(UserRepository userRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Retorna o perfil completo (com email) do usuário autenticado.
     */
    public ProfileResponse getMyProfile(UUID userId) {
        User user = findActiveUser(userId);
        return ProfileResponse.fromUser(user);
    }

    /**
     * Retorna o perfil público (sem email) de outro usuário.
     * Adequado para exibição pública — email e dados sensíveis omitidos.
     */
    public ProfileResponse getPublicProfile(UUID userId) {
        User user = findActiveUser(userId);
        return ProfileResponse.fromUserPublic(user);
    }

    /**
     * Atualiza campos do perfil.
     * Apenas campos não-nulos do request são aplicados (patch semântico).
     * Publica {@link ProfileUpdatedEvent} após salvar.
     */
    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = findActiveUser(userId);

        if (request.locale() != null) {
            user.setLocale(request.locale());
        }

        userRepository.save(user);
        eventPublisher.publishEvent(new ProfileUpdatedEvent(userId));

        return ProfileResponse.fromUser(user);
    }

    // ── helpers ───────────────────────────────────────────────

    private User findActiveUser(UUID userId) {
        return userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
