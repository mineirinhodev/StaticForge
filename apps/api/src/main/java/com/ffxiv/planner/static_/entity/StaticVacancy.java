package com.ffxiv.planner.static_.entity;

import com.ffxiv.planner.auth.entity.Role;
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
@Table(name = "static_vacancies")
@Getter
@Setter
@NoArgsConstructor
public class StaticVacancy extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "static_id", nullable = false)
    private StaticGroup staticGroup;

    @Column(name = "role_needed", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Role roleNeeded;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "job_preferences", columnDefinition = "jsonb")
    private String jobPreferences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements", columnDefinition = "jsonb")
    private String requirements;

    @Column(length = 500)
    private String description;

    @Column(name = "is_open", nullable = false)
    private boolean isOpen = true;

    @OneToMany(mappedBy = "vacancy", cascade = CascadeType.ALL)
    private List<Application> applications = new ArrayList<>();
}
