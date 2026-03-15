package com.ffxiv.planner.profile.dto;

import com.ffxiv.planner.auth.entity.GameCharacter;

import java.util.List;
import java.util.UUID;

public record CharacterResponse(
        UUID id,
        String name,
        String server,
        String dataCenter,
        Long lodestoneId,
        boolean isVerified,
        boolean isMain,
        String avatarUrl,
        String race,
        String gender,
        String freeCompany,
        String bio,
        boolean lookingForStatic,
        String availability,
        List<JobResponse> jobs
) {
    public static CharacterResponse from(GameCharacter character) {
        List<JobResponse> jobResponses = character.getJobs().stream()
                .map(JobResponse::from)
                .toList();
        return new CharacterResponse(
                character.getId(),
                character.getName(),
                character.getServer(),
                character.getDataCenter(),
                character.getLodestoneId(),
                character.isVerified(),
                character.isMain(),
                character.getAvatarUrl(),
                character.getRace(),
                character.getGender(),
                character.getFreeCompany(),
                character.getBio(),
                character.isLookingForStatic(),
                character.getAvailability(),
                jobResponses
        );
    }
}
