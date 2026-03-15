package com.ffxiv.planner.common.exception;

import java.util.UUID;

/**
 * Lançada quando um recurso solicitado não é encontrado ou não está acessível.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, UUID id) {
        super(resourceType + " não encontrado: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
