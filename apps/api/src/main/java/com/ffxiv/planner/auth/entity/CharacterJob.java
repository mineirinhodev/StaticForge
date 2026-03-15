package com.ffxiv.planner.auth.entity;

import com.ffxiv.planner.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "character_jobs")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CharacterJob extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private GameCharacter character;

    @Column(name = "job_name", nullable = false, length = 3)
    private String jobName;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_main", nullable = false)
    private boolean isMain = false;

    @Column
    private Integer ilvl;

    @Column(name = "parse_tier", length = 20)
    private String parseTier;

    @Column(length = 100)
    private String progression;
}
