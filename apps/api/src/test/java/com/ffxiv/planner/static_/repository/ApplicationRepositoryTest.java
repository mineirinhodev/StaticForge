package com.ffxiv.planner.static_.repository;

import com.ffxiv.planner.auth.DataJpaTestBase;
import com.ffxiv.planner.auth.entity.GameCharacter;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ApplicationRepositoryTest extends DataJpaTestBase {

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
    ApplicationRepository applicationRepository;

    private StaticGroup staticGroup;
    private StaticVacancy vacancy;
    private GameCharacter applicant;
    private Application pendingApp;
    private Application acceptedApp;
    private Application expiredApp;

    @BeforeEach
    void setUp() {
        User user = em.persist(minimalUser("applicant@test.com"));
        applicant = em.persist(minimalCharacter(user, "Applicant"));

        staticGroup = new StaticGroup();
        staticGroup.setName("Test Static");
        staticGroup.setStatus(StaticStatus.RECRUITING);
        staticGroup = em.persist(staticGroup);

        vacancy = new StaticVacancy();
        vacancy.setStaticGroup(staticGroup);
        vacancy.setRoleNeeded(Role.DPS);
        vacancy.setOpen(true);
        vacancy = em.persist(vacancy);

        pendingApp = new Application();
        pendingApp.setVacancy(vacancy);
        pendingApp.setCharacter(applicant);
        pendingApp.setStatus(ApplicationStatus.PENDING);
        pendingApp.setAppliedAt(Instant.now());
        pendingApp = em.persist(pendingApp);

        acceptedApp = new Application();
        acceptedApp.setVacancy(vacancy);
        acceptedApp.setCharacter(applicant);
        acceptedApp.setStatus(ApplicationStatus.ACCEPTED);
        acceptedApp.setAppliedAt(Instant.now().minusSeconds(3600));
        acceptedApp.setRespondedAt(Instant.now());
        acceptedApp = em.persist(acceptedApp);

        expiredApp = new Application();
        expiredApp.setVacancy(vacancy);
        expiredApp.setCharacter(applicant);
        expiredApp.setStatus(ApplicationStatus.EXPIRED);
        expiredApp.setAppliedAt(Instant.now().minus(java.time.Duration.ofDays(8)));
        expiredApp = em.persist(expiredApp);

        em.flush();
    }

    @Test
    void findPendingByStaticId_returnsOnlyPendingApplications() {
        List<Application> result = applicationRepository.findPendingByStaticId(staticGroup.getId());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void findByCharacterId_returnsAllApplicationsForCharacter() {
        List<Application> result = applicationRepository.findByCharacterId(applicant.getId());
        assertThat(result).hasSize(3);
    }

    @Test
    void findExpired_returnsExpiredApplications() {
        List<Application> result = applicationRepository.findExpired();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
    }

    @Test
    void findPendingByStaticId_returnsEmptyWhenNoPendingApplications() {
        pendingApp.setStatus(ApplicationStatus.REJECTED);
        em.persist(pendingApp);
        em.flush();

        List<Application> result = applicationRepository.findPendingByStaticId(staticGroup.getId());
        assertThat(result).isEmpty();
    }
}
