package com.ffxiv.planner.auth.entity;

import com.ffxiv.planner.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
public class GameCharacter extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String server;

    @Column(name = "data_center", nullable = false, length = 20)
    private String dataCenter;

    @Column(name = "lodestone_id", unique = true)
    private Long lodestoneId;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "is_main", nullable = false)
    private boolean isMain = false;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 20)
    private String race;

    @Column(length = 20)
    private String gender;

    @Column(name = "free_company", length = 100)
    private String freeCompany;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "looking_for_static", nullable = false)
    private boolean lookingForStatic = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "availability", columnDefinition = "jsonb")
    private String availability;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterJob> jobs = new ArrayList<>();
}
