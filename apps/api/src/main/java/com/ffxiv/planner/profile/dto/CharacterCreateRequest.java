package com.ffxiv.planner.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CharacterCreateRequest(
        @NotBlank @Size(max = 50)
        String name,

        @NotBlank @Size(max = 50)
        String server,

        @NotBlank @Size(max = 20)
        String dataCenter,

        boolean isMain,

        boolean lookingForStatic,

        String availability  // JSON de disponibilidade por dia da semana
) {}
