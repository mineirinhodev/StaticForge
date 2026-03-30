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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class StaticRepositoryTest extends DataJpaTestBase {

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
    StaticRepository staticRepository;

    @Autowired
    StaticMembershipRepository membershipRepository;

    private User user1;
    private User user2;
    private GameCharacter char1;
    private GameCharacter char2;
    private StaticGroup static1;
    private StaticGroup static2;

    @BeforeEach
    void setUp() {
        user1 = em.persist(minimalUser("leader@test.com"));
        user2 = em.persist(minimalUser("member@test.com"));
        char1 = em.persist(minimalCharacter(user1, "Leader Char"));
        char2 = em.persist(minimalCharacter(user2, "Member Char"));

        static1 = new StaticGroup();
        static1.setName("Test Static Alpha");
        static1.setStatus(StaticStatus.RECRUITING);
        static1.setDataCenter("Elemental");
        static1.setLanguage("en");
        static1 = em.persist(static1);

        static2 = new StaticGroup();
        static2.setName("Another Static Beta");
        static2.setStatus(StaticStatus.ACTIVE);
        static2.setDataCenter("Mana");
        static2.setLanguage("ja");
        static2 = em.persist(static2);

        // Add leader membership
        StaticMembership membership = new StaticMembership();
        membership.setStaticGroup(static1);
        membership.setCharacter(char1);
        membership.setRole(Role.TANK);
        membership.setLeader(true);
        membership.setJoinedAt(Instant.now());
        em.persist(membership);

        // Add member to static2
        StaticMembership membership2 = new StaticMembership();
        membership2.setStaticGroup(static2);
        membership2.setCharacter(char1);
        membership2.setRole(Role.TANK);
        membership2.setLeader(true);
        membership2.setJoinedAt(Instant.now());
        em.persist(membership2);

        em.flush();
    }

    @Test
    void findActiveById_returnsNonDeletedStatic() {
        Optional<StaticGroup> result = staticRepository.findActiveById(static1.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Static Alpha");
    }

    @Test
    void findActiveById_returnsEmptyForDeletedStatic() {
        static1.setDeletedAt(Instant.now());
        em.persist(static1);
        em.flush();

        Optional<StaticGroup> result = staticRepository.findActiveById(static1.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByMemberUserId_returnsStaticsWhereUserIsMember() {
        List<StaticGroup> result = staticRepository.findByMemberUserId(user1.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    void findByMemberUserId_returnsEmptyWhenUserHasNoMembership() {
        List<StaticGroup> result = staticRepository.findByMemberUserId(user2.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void countByLeaderUserId_countsStaticsWhereUserIsLeader() {
        long count = staticRepository.countByLeaderUserId(user1.getId());
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void specification_filterByDataCenter() {
        Specification<StaticGroup> spec = StaticSpecifications.hasDataCenter("Elemental");
        List<StaticGroup> result = staticRepository.findAll(spec);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDataCenter()).isEqualTo("Elemental");
    }

    @Test
    void specification_filterByStatus() {
        Specification<StaticGroup> spec = StaticSpecifications.hasStatus(StaticStatus.RECRUITING);
        List<StaticGroup> result = staticRepository.findAll(spec);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StaticStatus.RECRUITING);
    }

    @Test
    void specification_filterByLanguage() {
        Specification<StaticGroup> spec = StaticSpecifications.hasLanguage("ja");
        List<StaticGroup> result = staticRepository.findAll(spec);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLanguage()).isEqualTo("ja");
    }

    @Test
    void specification_filterByNameContaining() {
        Specification<StaticGroup> spec = StaticSpecifications.nameContains("Alpha");
        List<StaticGroup> result = staticRepository.findAll(spec);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("Alpha");
    }

    @Test
    void specification_isNotDeleted() {
        static1.setDeletedAt(Instant.now());
        em.persist(static1);
        em.flush();

        Specification<StaticGroup> spec = StaticSpecifications.isNotDeleted();
        List<StaticGroup> result = staticRepository.findAll(spec);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(static2.getId());
    }

    @Test
    void specification_combinedFilters() {
        Specification<StaticGroup> spec = StaticSpecifications.isNotDeleted()
                .and(StaticSpecifications.hasDataCenter("Elemental"))
                .and(StaticSpecifications.hasStatus(StaticStatus.RECRUITING));

        List<StaticGroup> result = staticRepository.findAll(spec);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Static Alpha");
    }
}
