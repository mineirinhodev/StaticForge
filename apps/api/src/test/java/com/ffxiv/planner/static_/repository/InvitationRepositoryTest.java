package com.ffxiv.planner.static_.repository;

import com.ffxiv.planner.auth.DataJpaTestBase;
import com.ffxiv.planner.auth.entity.GameCharacter;
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
class InvitationRepositoryTest extends DataJpaTestBase {

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
    InvitationRepository invitationRepository;

    private StaticGroup staticGroup;
    private GameCharacter invitee;
    private Invitation pendingInvitation;
    private Invitation expiredInvitation;
    private Invitation acceptedInvitation;

    @BeforeEach
    void setUp() {
        User user = em.persist(minimalUser("invitee@test.com"));
        invitee = em.persist(minimalCharacter(user, "Invitee"));

        staticGroup = new StaticGroup();
        staticGroup.setName("Test Static");
        staticGroup.setStatus(StaticStatus.RECRUITING);
        staticGroup = em.persist(staticGroup);

        pendingInvitation = new Invitation();
        pendingInvitation.setStaticGroup(staticGroup);
        pendingInvitation.setCharacter(invitee);
        pendingInvitation.setStatus(InvitationStatus.PENDING);
        pendingInvitation.setSentAt(Instant.now());
        pendingInvitation = em.persist(pendingInvitation);

        expiredInvitation = new Invitation();
        expiredInvitation.setStaticGroup(staticGroup);
        expiredInvitation.setCharacter(invitee);
        expiredInvitation.setStatus(InvitationStatus.EXPIRED);
        expiredInvitation.setSentAt(Instant.now().minus(java.time.Duration.ofDays(8)));
        expiredInvitation = em.persist(expiredInvitation);

        acceptedInvitation = new Invitation();
        acceptedInvitation.setStaticGroup(staticGroup);
        acceptedInvitation.setCharacter(invitee);
        acceptedInvitation.setStatus(InvitationStatus.ACCEPTED);
        acceptedInvitation.setSentAt(Instant.now().minusSeconds(3600));
        acceptedInvitation.setRespondedAt(Instant.now());
        acceptedInvitation = em.persist(acceptedInvitation);

        em.flush();
    }

    @Test
    void findPendingByUserId_returnsPendingInvitationsForUser() {
        List<Invitation> result = invitationRepository.findPendingByUserId(invitee.getUser().getId());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void findPendingByUserId_returnsEmptyWhenNoPendingInvitations() {
        pendingInvitation.setStatus(InvitationStatus.REJECTED);
        em.persist(pendingInvitation);
        em.flush();

        List<Invitation> result = invitationRepository.findPendingByUserId(invitee.getUser().getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findExpired_returnsExpiredInvitations() {
        List<Invitation> result = invitationRepository.findExpired();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }
}
