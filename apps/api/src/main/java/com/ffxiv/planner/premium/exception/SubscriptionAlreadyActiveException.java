package com.ffxiv.planner.premium.exception;

/**
 * Lançada quando o usuário já possui uma assinatura ativa.
 */
public class SubscriptionAlreadyActiveException extends IllegalStateException {
    public SubscriptionAlreadyActiveException() {
        super("Usuário já possui uma assinatura ativa");
    }
}
