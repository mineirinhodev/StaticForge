package com.ffxiv.planner.static_.entity;

import com.ffxiv.planner.auth.entity.GameCharacter;
import com.ffxiv.planner.auth.entity.Role;
import com.ffxiv.planner.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "static_memberships")
@Getter
@Setter
@NoArgsConstructor
public class StaticMembership extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "static_id", nullable = false)
    private StaticGroup staticGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private GameCharacter character;

    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "job_assigned", length = 3)
    private String jobAssigned;

    @Column(name = "is_leader", nullable = false)
    private boolean isLeader = false;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;
}
