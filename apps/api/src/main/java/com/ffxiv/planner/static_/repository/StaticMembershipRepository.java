package com.ffxiv.planner.static_.repository;

import com.ffxiv.planner.static_.entity.StaticMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaticMembershipRepository extends JpaRepository<StaticMembership, UUID> {

    @Query("SELECT m FROM StaticMembership m WHERE m.staticGroup.id = :staticId AND m.leftAt IS NULL")
    List<StaticMembership> findActiveByStaticId(@Param("staticId") UUID staticId);

    @Query("SELECT m FROM StaticMembership m WHERE m.staticGroup.id = :staticId AND m.isLeader = true AND m.leftAt IS NULL")
    Optional<StaticMembership> findLeaderByStaticId(@Param("staticId") UUID staticId);

    @Query("SELECT COUNT(m) FROM StaticMembership m WHERE m.staticGroup.id = :staticId AND m.leftAt IS NULL")
    long countActive(@Param("staticId") UUID staticId);
}
