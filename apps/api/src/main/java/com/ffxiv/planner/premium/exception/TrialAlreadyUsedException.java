package com.ffxiv.planner.premium.exception;

/**
 * Lançada quando o trial já foi utilizado (RN45 — trial único por conta).
 */
public class TrialAlreadyUsedException extends IllegalStateException {
    public TrialAlreadyUsedException() {
        super("O período de trial já foi utilizado nesta conta");
    }
}
