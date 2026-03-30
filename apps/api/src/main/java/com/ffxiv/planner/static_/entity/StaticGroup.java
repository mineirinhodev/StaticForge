package com.ffxiv.planner.static_.entity;

import com.ffxiv.planner.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Static: grupo fixo de raid com até 8 membros.
 *
 * <p>O nome da classe é {@code StaticGroup} para evitar conflito com a
 * keyword {@code static} do Java. A tabela continua sendo {@code statics}.</p>
 */
@Entity
@Table(name = "statics")
@Getter
@Setter
@NoArgsConstructor
public class StaticGroup extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_scope", columnDefinition = "jsonb")
    private String contentScope;

    @Column(name = "loot_rules", length = 500)
    private String lootRules;

    @Column(length = 10)
    private String language;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StaticStatus status = StaticStatus.RECRUITING;

    @Column(name = "max_members", nullable = false)
    private int maxMembers = 8;

    @Column(name = "data_center", length = 20)
    private String dataCenter;

    @Column(length = 50)
    private String server;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String schedule;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "staticGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StaticMembership> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "staticGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StaticVacancy> vacancies = new ArrayList<>();

    @OneToMany(mappedBy = "staticGroup", cascade = CascadeType.ALL)
    private List<Invitation> invitations = new ArrayList<>();
}
