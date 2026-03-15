package com.ffxiv.planner.profile.integration;

/**
 * Dados do personagem retornados pelo XIVAPI / Lodestone.
 */
public record LodestoneCharacterData(
        Long lodestoneId,
        String name,
        String server,
        String bio,
        String avatarUrl,
        String race,
        String gender,
        String freeCompany
) {}
