package com.ffxiv.planner.profile.dto;

import com.ffxiv.planner.auth.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resposta de perfil completo.
 *
 * <p>Para perfis públicos, {@code email} é {@code null}.
 * Para o próprio perfil ({@code GET /profile/me}), email é incluído.
 */
public record ProfileResponse(
        UUID id,
        String email,      // null para perfis públicos
        String locale,
        boolean isPremium,
        Instant premiumExpiresAt,
        List<CharacterResponse> characters,
        Instant createdAt,
        Instant updatedAt
) {
    /** Mapeia perfil completo (com email) para o usuário autenticado. */
    public static ProfileResponse fromUser(User user) {
        List<CharacterResponse> chars = user.getCharacters().stream()
                .map(CharacterResponse::from)
                .toList();
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getLocale(),
                user.isPremium(),
                user.getPremiumExpiresAt(),
                chars,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /** Mapeia perfil público (sem email) para outros usuários. */
    public static ProfileResponse fromUserPublic(User user) {
        List<CharacterResponse> chars = user.getCharacters().stream()
                .map(CharacterResponse::from)
                .toList();
        return new ProfileResponse(
                user.getId(),
                null,          // email omitido em perfis públicos
                user.getLocale(),
                user.isPremium(),
                null,          // premiumExpiresAt omitido em perfis públicos
                chars,
                null,
                null
        );
    }
}
