package com.ffxiv.planner.premium.exception;

import com.ffxiv.planner.premium.entity.SubscriptionStatus;

import java.time.LocalDate;

/**
 * Lançada quando a assinatura não pode ser reativada.
 *
 * <p>Usar os métodos fábrica para distinguir os dois casos:</p>
 * <ul>
 *   <li>{@link #invalidStatus(SubscriptionStatus)} — status não é CANCELED</li>
 *   <li>{@link #alreadyExpired(LocalDate)} — endDate já passou</li>
 * </ul>
 */
public class CannotReactivateSubscriptionException extends IllegalStateException {

    private CannotReactivateSubscriptionException(String message) {
        super(message);
    }

    public static CannotReactivateSubscriptionException invalidStatus(SubscriptionStatus status) {
        return new CannotReactivateSubscriptionException(
                "Não é possível reativar a assinatura: status atual é " + status.name());
    }

    public static CannotReactivateSubscriptionException alreadyExpired(LocalDate endDate) {
        return new CannotReactivateSubscriptionException(
                "Não é possível reativar a assinatura: já expirou em " + endDate);
    }
}
