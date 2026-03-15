package com.ffxiv.planner.common.exception;

/**
 * Lançada quando o usuário autenticado tenta operar um recurso que não lhe pertence.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException() {
        super("Acesso negado ao recurso solicitado");
    }
}
