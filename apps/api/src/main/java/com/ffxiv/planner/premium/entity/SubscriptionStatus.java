package com.ffxiv.planner.premium.entity;

/**
 * Estados possíveis de uma assinatura premium.
 */
public enum SubscriptionStatus {
    ACTIVE,
    CANCELED,   // cancelado, mas ativo até o fim do período
    PAST_DUE,   // pagamento falhou, em período de graça
    EXPIRED,    // expirado, sem acesso premium
    TRIALING;   // período de trial (7 dias)

    /**
     * Retorna {@code true} para os estados que concedem acesso premium ativo.
     *
     * <p>Usado pelo serviço para validar transições de estado e pelo
     * scheduler para detectar assinaturas expiradas.</p>
     */
    public boolean isActive() {
        return this == ACTIVE || this == TRIALING || this == PAST_DUE;
    }
}
