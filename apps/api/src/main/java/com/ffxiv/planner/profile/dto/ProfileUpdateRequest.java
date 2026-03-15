package com.ffxiv.planner.profile.dto;

import jakarta.validation.constraints.Size;

/**
 * Requisição para atualizar campos do perfil do usuário.
 * Todos os campos são opcionais (null = manter valor atual).
 */
public record ProfileUpdateRequest(
        @Size(max = 10)
        String locale,

        @Size(max = 1000)
        String bio,

        String availability  // JSON de disponibilidade por dia da semana
) {}
