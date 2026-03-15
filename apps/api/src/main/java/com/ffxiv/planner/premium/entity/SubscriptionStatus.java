package com.ffxiv.planner.premium.entity;

/**
 * Estados possíveis de uma assinatura premium.
 */
public enum SubscriptionStatus {
    ACTIVE,
    CANCELED,   // cancelado, mas ativo até o fim do período
    PAST_DUE,   // pagamento falhou, em período de graça
    EXPIRED,    // expirado, sem acesso premium
    TRIALING    // período de trial (7 dias)
}
