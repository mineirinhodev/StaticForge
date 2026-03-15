package com.ffxiv.planner.profile.event;

import java.util.UUID;

/**
 * Publicado quando a verificação Lodestone de um personagem é confirmada.
 * Consumido por: {@code MatchingService} (ativar matching), {@code NotificationService}.
 */
public record CharacterVerifiedEvent(UUID characterId, UUID userId) {}
