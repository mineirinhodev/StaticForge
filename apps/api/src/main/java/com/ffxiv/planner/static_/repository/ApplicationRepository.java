package com.ffxiv.planner.static_.repository;

import com.ffxiv.planner.static_.entity.Application;
import com.ffxiv.planner.static_.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @Query("""
            SELECT a FROM Application a
            WHERE a.vacancy.staticGroup.id = :staticId
            AND a.status = com.ffxiv.planner.static_.entity.ApplicationStatus.PENDING
            """)
    List<Application> findPendingByStaticId(@Param("staticId") UUID staticId);

    @Query("SELECT a FROM Application a WHERE a.character.id = :characterId")
    List<Application> findByCharacterId(@Param("characterId") UUID characterId);

    @Query("SELECT a FROM Application a WHERE a.status = com.ffxiv.planner.static_.entity.ApplicationStatus.EXPIRED")
    List<Application> findExpired();
}
