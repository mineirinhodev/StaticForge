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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class StaticMembershipRepositoryTest extends DataJpaTestBase {

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
    StaticMembershipRepository membershipRepository;

    private StaticGroup staticGroup;
    private GameCharacter leader;
    private GameCharacter member;
    private StaticMembership leaderMembership;
    private StaticMembership memberMembership;

    @BeforeEach
    void setUp() {
        User user1 = em.persist(minimalUser("leader@test.com"));
        User user2 = em.persist(minimalUser("member@test.com"));
        leader = em.persist(minimalCharacter(user1, "Leader"));
        member = em.persist(minimalCharacter(user2, "Member"));

        staticGroup = new StaticGroup();
        staticGroup.setName("Test Static");
        staticGroup.setStatus(StaticStatus.RECRUITING);
        staticGroup = em.persist(staticGroup);

        leaderMembership = new StaticMembership();
        leaderMembership.setStaticGroup(staticGroup);
        leaderMembership.setCharacter(leader);
        leaderMembership.setRole(Role.TANK);
        leaderMembership.setLeader(true);
        leaderMembership.setJoinedAt(Instant.now());
        leaderMembership = em.persist(leaderMembership);

        memberMembership = new StaticMembership();
        memberMembership.setStaticGroup(staticGroup);
        memberMembership.setCharacter(member);
        memberMembership.setRole(Role.HEALER);
        memberMembership.setLeader(false);
        memberMembership.setJoinedAt(Instant.now());
        memberMembership = em.persist(memberMembership);

        em.flush();
    }

    @Test
    void findActiveByStaticId_returnsActiveMemberships() {
        List<StaticMembership> result = membershipRepository.findActiveByStaticId(staticGroup.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    void findActiveByStaticId_excludesLeftMembers() {
        memberMembership.setLeftAt(Instant.now());
        em.persist(memberMembership);
        em.flush();

        List<StaticMembership> result = membershipRepository.findActiveByStaticId(staticGroup.getId());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isLeader()).isTrue();
    }

    @Test
    void findLeaderByStaticId_returnsLeader() {
        Optional<StaticMembership> result = membershipRepository.findLeaderByStaticId(staticGroup.getId());
        assertThat(result).isPresent();
        assertThat(result.get().isLeader()).isTrue();
        assertThat(result.get().getCharacter().getName()).isEqualTo("Leader");
    }

    @Test
    void countActive_returnsCountOfActiveMemberships() {
        long count = membershipRepository.countActive(staticGroup.getId());
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countActive_excludesLeftMembers() {
        memberMembership.setLeftAt(Instant.now());
        em.persist(memberMembership);
        em.flush();

        long count = membershipRepository.countActive(staticGroup.getId());
        assertThat(count).isEqualTo(1L);
    }
}
