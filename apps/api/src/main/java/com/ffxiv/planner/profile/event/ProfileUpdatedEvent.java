package com.ffxiv.planner.profile.event;

import java.util.UUID;

/**
 * Publicado quando o perfil de um usuário é atualizado.
 * Consumido por: {@code MatchingService} (recalcular matches).
 */
public record ProfileUpdatedEvent(UUID userId) {}
