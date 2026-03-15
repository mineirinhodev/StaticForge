package com.ffxiv.planner.auth.repository;

import com.ffxiv.planner.auth.entity.GameCharacter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterRepository extends JpaRepository<GameCharacter, UUID> {

    List<GameCharacter> findByUserIdOrderByIsMainDesc(UUID userId);

    Optional<GameCharacter> findByLodestoneId(Long lodestoneId);

    boolean existsByLodestoneId(Long lodestoneId);

    @Query("SELECT c FROM GameCharacter c WHERE c.user.id = :userId AND c.isMain = true")
    Optional<GameCharacter> findMainByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM GameCharacter c WHERE c.dataCenter = :dataCenter AND c.lookingForStatic = true AND c.isVerified = true")
    Page<GameCharacter> findLFSByDataCenter(@Param("dataCenter") String dataCenter, Pageable pageable);
}
