package com.ffxiv.planner.premium.event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Publicado quando uma assinatura expira em até 3 dias.
 * Usado para disparar notificações preventivas ao usuário.
 */
public record SubscriptionExpiringSoonEvent(UUID userId, LocalDate endDate) {}
