package com.ffxiv.planner.profile.event;

import java.util.UUID;

/**
 * Publicado quando um personagem é criado.
 */
public record CharacterCreatedEvent(UUID characterId, UUID userId) {}
