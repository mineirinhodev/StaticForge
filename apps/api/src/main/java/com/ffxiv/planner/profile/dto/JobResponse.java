package com.ffxiv.planner.profile.dto;

import com.ffxiv.planner.auth.entity.CharacterJob;
import com.ffxiv.planner.auth.entity.Role;

import java.util.UUID;

public record JobResponse(
        UUID id,
        String jobName,
        Role role,
        boolean isMain,
        Integer ilvl,
        String parseTier,
        String progression
) {
    public static JobResponse from(CharacterJob job) {
        return new JobResponse(
                job.getId(),
                job.getJobName(),
                job.getRole(),
                job.isMain(),
                job.getIlvl(),
                job.getParseTier(),
                job.getProgression()
        );
    }
}
