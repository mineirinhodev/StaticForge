package com.ffxiv.planner.static_.repository;

import com.ffxiv.planner.static_.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    @Query("""
            SELECT i FROM Invitation i
            WHERE i.character.user.id = :userId
            AND i.status = com.ffxiv.planner.static_.entity.InvitationStatus.PENDING
            """)
    List<Invitation> findPendingByUserId(@Param("userId") UUID userId);

    @Query("SELECT i FROM Invitation i WHERE i.status = com.ffxiv.planner.static_.entity.InvitationStatus.EXPIRED")
    List<Invitation> findExpired();
}
