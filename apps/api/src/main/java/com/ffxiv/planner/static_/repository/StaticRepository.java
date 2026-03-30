package com.ffxiv.planner.static_.repository;

import com.ffxiv.planner.static_.entity.StaticGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaticRepository extends JpaRepository<StaticGroup, UUID>, JpaSpecificationExecutor<StaticGroup> {

    @Query("SELECT s FROM StaticGroup s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<StaticGroup> findActiveById(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT s FROM StaticGroup s
            JOIN s.memberships m
            WHERE m.character.user.id = :userId
            AND m.leftAt IS NULL
            AND s.deletedAt IS NULL
            """)
    List<StaticGroup> findByMemberUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COUNT(DISTINCT s) FROM StaticGroup s
            JOIN s.memberships m
            WHERE m.character.user.id = :userId
            AND m.isLeader = true
            AND m.leftAt IS NULL
            AND s.deletedAt IS NULL
            """)
    long countByLeaderUserId(@Param("userId") UUID userId);
}
