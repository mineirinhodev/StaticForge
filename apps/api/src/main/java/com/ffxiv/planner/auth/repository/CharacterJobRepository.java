package com.ffxiv.planner.auth.repository;

import com.ffxiv.planner.auth.entity.CharacterJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterJobRepository extends JpaRepository<CharacterJob, UUID> {

    List<CharacterJob> findByCharacterId(UUID characterId);

    @Query("SELECT j FROM CharacterJob j WHERE j.character.id = :characterId AND j.isMain = true")
    Optional<CharacterJob> findMainByCharacterId(@Param("characterId") UUID characterId);

    boolean existsByCharacterIdAndJobName(UUID characterId, String jobName);
}
