package com.ffxiv.planner.static_.repository;

import com.ffxiv.planner.auth.DataJpaTestBase;
import com.ffxiv.planner.auth.entity.Role;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.static_.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class StaticVacancyRepositoryTest extends DataJpaTestBase {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    TestEntityManager em;

    @Autowired
    StaticVacancyRepository vacancyRepository;

    private StaticGroup staticGroup;
    private StaticVacancy openVacancy;
    private StaticVacancy closedVacancy;

    @BeforeEach
    void setUp() {
        staticGroup = new StaticGroup();
        staticGroup.setName("Test Static");
        staticGroup.setStatus(StaticStatus.RECRUITING);
        staticGroup = em.persist(staticGroup);

        openVacancy = new StaticVacancy();
        openVacancy.setStaticGroup(staticGroup);
        openVacancy.setRoleNeeded(Role.TANK);
        openVacancy.setOpen(true);
        openVacancy = em.persist(openVacancy);

        closedVacancy = new StaticVacancy();
        closedVacancy.setStaticGroup(staticGroup);
        closedVacancy.setRoleNeeded(Role.HEALER);
        closedVacancy.setOpen(false);
        closedVacancy = em.persist(closedVacancy);

        em.flush();
    }

    @Test
    void findByStaticIdAndIsOpenTrue_returnsOnlyOpenVacancies() {
        List<StaticVacancy> result = vacancyRepository.findByStaticGroupIdAndIsOpenTrue(staticGroup.getId());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoleNeeded()).isEqualTo(Role.TANK);
        assertThat(result.get(0).isOpen()).isTrue();
    }

    @Test
    void findByStaticIdAndIsOpenTrue_returnsEmptyWhenNoOpenVacancies() {
        openVacancy.setOpen(false);
        em.persist(openVacancy);
        em.flush();

        List<StaticVacancy> result = vacancyRepository.findByStaticGroupIdAndIsOpenTrue(staticGroup.getId());
        assertThat(result).isEmpty();
    }
}
