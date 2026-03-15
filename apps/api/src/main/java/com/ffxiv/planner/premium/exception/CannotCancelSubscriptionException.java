package com.ffxiv.planner.premium.exception;

import com.ffxiv.planner.premium.entity.SubscriptionStatus;

/**
 * Lançada quando a assinatura não está em um estado que permite cancelamento.
 */
public class CannotCancelSubscriptionException extends IllegalStateException {
    public CannotCancelSubscriptionException(SubscriptionStatus status) {
        super("Não é possível cancelar uma assinatura com status: " + status.name());
    }
}
