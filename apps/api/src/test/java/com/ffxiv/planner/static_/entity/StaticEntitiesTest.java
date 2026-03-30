package com.ffxiv.planner.static_.entity;

import com.ffxiv.planner.auth.DataJpaTestBase;
import com.ffxiv.planner.auth.entity.GameCharacter;
import com.ffxiv.planner.auth.entity.Role;
import com.ffxiv.planner.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa persistência das entidades Static, StaticMembership, StaticVacancy,
 * Application e Invitation contra PostgreSQL real via Testcontainers.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/35
 */
@Testcontainers
class StaticEntitiesTest extends DataJpaTestBase {

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
    private TestEntityManager em;

    // ── Static ────────────────────────────────────────────────

    @Test
    void staticCanBePersistedWithDefaults() {
        StaticGroup saved = em.persistFlushFind(minimalStatic("Savage Static"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Savage Static");
        assertThat(saved.getStatus()).isEqualTo(StaticStatus.RECRUITING);
        assertThat(saved.getMaxMembers()).isEqualTo(8);
        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    void staticAuditFieldsArePopulatedOnPersist() {
        StaticGroup saved = em.persistFlushFind(minimalStatic("Audit Static"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void staticStatusEnumIsPersistedAsString() {
        StaticGroup s = minimalStatic("Active Static");
        s.setStatus(StaticStatus.ACTIVE);
        StaticGroup saved = em.persistFlushFind(s);

        assertThat(saved.getStatus()).isEqualTo(StaticStatus.ACTIVE);
    }

    @Test
    void staticWithOptionalFieldsCanBePersisted() {
        StaticGroup s = minimalStatic("Full Static");
        s.setDescription("Prog group for TOP");
        s.setLootRules("Loot council");
        s.setLanguage("en");
        s.setDataCenter("Elemental");
        s.setServer("Tonberry");
        s.setContentScope("[\"ULTIMATE\"]");
        s.setSchedule("{\"days\":[\"SAT\",\"SUN\"]}");
        StaticGroup saved = em.persistFlushFind(s);

        assertThat(saved.getDescription()).isEqualTo("Prog group for TOP");
        assertThat(saved.getDataCenter()).isEqualTo("Elemental");
        assertThat(saved.getContentScope()).contains("ULTIMATE");
    }

    @Test
    void staticSoftDeleteFieldIsNullByDefault() {
        StaticGroup saved = em.persistFlushFind(minimalStatic("Soft Delete"));

        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    void staticAllStatusValuesCanBePersisted() {
        for (StaticStatus status : StaticStatus.values()) {
            StaticGroup s = minimalStatic("Static-" + status.name());
            s.setStatus(status);
            StaticGroup saved = em.persistFlushFind(s);
            assertThat(saved.getStatus()).isEqualTo(status);
        }
    }

    // ── StaticMembership ──────────────────────────────────────

    @Test
    void membershipCanBePersistedWithDefaults() {
        User user = em.persist(minimalUser("member@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "CharacterA"));
        StaticGroup group = em.persist(minimalStatic("Group A"));
        StaticMembership saved = em.persistFlushFind(minimalMembership(group, character));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStaticGroup().getId()).isEqualTo(group.getId());
        assertThat(saved.getCharacter().getId()).isEqualTo(character.getId());
        assertThat(saved.isLeader()).isFalse();
        assertThat(saved.getLeftAt()).isNull();
    }

    @Test
    void membershipAuditFieldsArePopulatedOnPersist() {
        User user = em.persist(minimalUser("member-audit@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "CharacterAudit"));
        StaticGroup group = em.persist(minimalStatic("Group Audit"));
        StaticMembership saved = em.persistFlushFind(minimalMembership(group, character));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void membershipLeaderCanBePersisted() {
        User user = em.persist(minimalUser("leader@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Leader"));
        StaticGroup group = em.persist(minimalStatic("Group Leader"));
        StaticMembership membership = minimalMembership(group, character);
        membership.setLeader(true);
        StaticMembership saved = em.persistFlushFind(membership);

        assertThat(saved.isLeader()).isTrue();
    }

    @Test
    void membershipRoleEnumIsPersistedAsString() {
        User user = em.persist(minimalUser("tank@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Tank"));
        StaticGroup group = em.persist(minimalStatic("Tank Group"));
        StaticMembership membership = minimalMembership(group, character);
        membership.setRole(Role.TANK);
        membership.setJobAssigned("PLD");
        StaticMembership saved = em.persistFlushFind(membership);

        assertThat(saved.getRole()).isEqualTo(Role.TANK);
        assertThat(saved.getJobAssigned()).isEqualTo("PLD");
    }

    @Test
    void membershipLeftAtCanBeSet() {
        User user = em.persist(minimalUser("left@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "LeftMember"));
        StaticGroup group = em.persist(minimalStatic("Left Group"));
        StaticMembership membership = minimalMembership(group, character);
        Instant leftAt = Instant.now();
        membership.setLeftAt(leftAt);
        StaticMembership saved = em.persistFlushFind(membership);

        assertThat(saved.getLeftAt()).isNotNull();
    }

    // ── StaticVacancy ─────────────────────────────────────────

    @Test
    void vacancyCanBePersistedWithDefaults() {
        StaticGroup group = em.persist(minimalStatic("Vacancy Group"));
        StaticVacancy saved = em.persistFlushFind(minimalVacancy(group, Role.HEALER));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStaticGroup().getId()).isEqualTo(group.getId());
        assertThat(saved.getRoleNeeded()).isEqualTo(Role.HEALER);
        assertThat(saved.isOpen()).isTrue();
    }

    @Test
    void vacancyAuditFieldsArePopulatedOnPersist() {
        StaticGroup group = em.persist(minimalStatic("Vacancy Audit Group"));
        StaticVacancy saved = em.persistFlushFind(minimalVacancy(group, Role.DPS));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void vacancyWithJsonbFieldsCanBePersisted() {
        StaticGroup group = em.persist(minimalStatic("JSONB Group"));
        StaticVacancy vacancy = minimalVacancy(group, Role.TANK);
        vacancy.setJobPreferences("[\"PLD\",\"WAR\"]");
        vacancy.setRequirements("{\"minIlvl\":660}");
        vacancy.setDescription("MT spot open");
        StaticVacancy saved = em.persistFlushFind(vacancy);

        assertThat(saved.getJobPreferences()).contains("PLD").contains("WAR");
        assertThat(saved.getRequirements()).contains("minIlvl").contains("660");
        assertThat(saved.getDescription()).isEqualTo("MT spot open");
    }

    @Test
    void vacancyCanBeClosed() {
        StaticGroup group = em.persist(minimalStatic("Closed Group"));
        StaticVacancy vacancy = minimalVacancy(group, Role.DPS);
        vacancy.setOpen(false);
        StaticVacancy saved = em.persistFlushFind(vacancy);

        assertThat(saved.isOpen()).isFalse();
    }

    // ── Application ───────────────────────────────────────────

    @Test
    void applicationCanBePersistedWithDefaults() {
        User user = em.persist(minimalUser("applicant@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Applicant"));
        StaticGroup group = em.persist(minimalStatic("App Group"));
        StaticVacancy vacancy = em.persist(minimalVacancy(group, Role.DPS));
        Application saved = em.persistFlushFind(minimalApplication(vacancy, character));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVacancy().getId()).isEqualTo(vacancy.getId());
        assertThat(saved.getCharacter().getId()).isEqualTo(character.getId());
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(saved.getAppliedAt()).isNotNull();
        assertThat(saved.getRespondedAt()).isNull();
    }

    @Test
    void applicationAuditFieldsArePopulatedOnPersist() {
        User user = em.persist(minimalUser("app-audit@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "AppAudit"));
        StaticGroup group = em.persist(minimalStatic("AppAudit Group"));
        StaticVacancy vacancy = em.persist(minimalVacancy(group, Role.DPS));
        Application saved = em.persistFlushFind(minimalApplication(vacancy, character));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void applicationStatusEnumIsPersistedAsString() {
        User user = em.persist(minimalUser("appstatus@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "AppStatus"));
        StaticGroup group = em.persist(minimalStatic("Status Group"));
        StaticVacancy vacancy = em.persist(minimalVacancy(group, Role.HEALER));
        Application application = minimalApplication(vacancy, character);
        application.setStatus(ApplicationStatus.ACCEPTED);
        Application saved = em.persistFlushFind(application);

        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void applicationAllStatusValuesCanBePersisted() {
        int i = 0;
        for (ApplicationStatus status : ApplicationStatus.values()) {
            User user = em.persist(minimalUser("appstatus" + i + "@test.com"));
            GameCharacter character = em.persist(minimalCharacter(user, "AppStatus" + i));
            StaticGroup group = em.persist(minimalStatic("AppStatusGroup-" + i));
            StaticVacancy vacancy = em.persist(minimalVacancy(group, Role.DPS));
            Application application = minimalApplication(vacancy, character);
            application.setStatus(status);
            Application saved = em.persistFlushFind(application);
            assertThat(saved.getStatus()).isEqualTo(status);
            i++;
        }
    }

    @Test
    void applicationWithResponseCanBePersisted() {
        User user = em.persist(minimalUser("respond@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Respond"));
        StaticGroup group = em.persist(minimalStatic("Respond Group"));
        StaticVacancy vacancy = em.persist(minimalVacancy(group, Role.TANK));
        Application application = minimalApplication(vacancy, character);
        application.setMessage("Please accept me!");
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRespondedAt(Instant.now());
        application.setResponseMessage("Sorry, full.");
        Application saved = em.persistFlushFind(application);

        assertThat(saved.getMessage()).isEqualTo("Please accept me!");
        assertThat(saved.getResponseMessage()).isEqualTo("Sorry, full.");
        assertThat(saved.getRespondedAt()).isNotNull();
    }

    // ── Invitation ────────────────────────────────────────────

    @Test
    void invitationCanBePersistedWithDefaults() {
        User user = em.persist(minimalUser("invited@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Invited"));
        StaticGroup group = em.persist(minimalStatic("Invite Group"));
        Invitation saved = em.persistFlushFind(minimalInvitation(group, character));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStaticGroup().getId()).isEqualTo(group.getId());
        assertThat(saved.getCharacter().getId()).isEqualTo(character.getId());
        assertThat(saved.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(saved.getSentAt()).isNotNull();
        assertThat(saved.getRespondedAt()).isNull();
        assertThat(saved.getVacancyId()).isNull();
    }

    @Test
    void invitationAuditFieldsArePopulatedOnPersist() {
        User user = em.persist(minimalUser("inv-audit@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "InvAudit"));
        StaticGroup group = em.persist(minimalStatic("InvAudit Group"));
        Invitation saved = em.persistFlushFind(minimalInvitation(group, character));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void invitationStatusEnumIsPersistedAsString() {
        User user = em.persist(minimalUser("invstatus@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "InvStatus"));
        StaticGroup group = em.persist(minimalStatic("InvStatus Group"));
        Invitation invitation = minimalInvitation(group, character);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        Invitation saved = em.persistFlushFind(invitation);

        assertThat(saved.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void invitationAllStatusValuesCanBePersisted() {
        int i = 0;
        for (InvitationStatus status : InvitationStatus.values()) {
            User user = em.persist(minimalUser("invs" + i + "@test.com"));
            GameCharacter character = em.persist(minimalCharacter(user, "InvS" + i));
            StaticGroup group = em.persist(minimalStatic("InvSGroup-" + i));
            Invitation invitation = minimalInvitation(group, character);
            invitation.setStatus(status);
            Invitation saved = em.persistFlushFind(invitation);
            assertThat(saved.getStatus()).isEqualTo(status);
            i++;
        }
    }

    @Test
    void invitationWithVacancyReferenceCanBePersisted() {
        User user = em.persist(minimalUser("invvac@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "InvVac"));
        StaticGroup group = em.persist(minimalStatic("InvVac Group"));
        StaticVacancy vacancy = em.persist(minimalVacancy(group, Role.HEALER));
        Invitation invitation = minimalInvitation(group, character);
        invitation.setVacancyId(vacancy.getId());
        Invitation saved = em.persistFlushFind(invitation);

        assertThat(saved.getVacancyId()).isEqualTo(vacancy.getId());
    }

    // ── Relationships ─────────────────────────────────────────

    @Test
    void staticHasMembershipsCollection() {
        User user = em.persist(minimalUser("rel-member@test.com"));
        GameCharacter c1 = em.persist(minimalCharacter(user, "RelMember1"));
        GameCharacter c2 = em.persist(minimalCharacter(user, "RelMember2"));
        StaticGroup group = em.persist(minimalStatic("Rel Group"));
        em.persist(minimalMembership(group, c1));
        em.persist(minimalMembership(group, c2));
        em.flush();
        em.clear();

        StaticGroup found = em.find(StaticGroup.class, group.getId());

        assertThat(found.getMemberships()).hasSize(2);
    }

    @Test
    void staticHasVacanciesCollection() {
        StaticGroup group = em.persist(minimalStatic("Vac Rel Group"));
        em.persist(minimalVacancy(group, Role.TANK));
        em.persist(minimalVacancy(group, Role.HEALER));
        em.flush();
        em.clear();

        StaticGroup found = em.find(StaticGroup.class, group.getId());

        assertThat(found.getVacancies()).hasSize(2);
    }

    @Test
    void vacancyHasApplicationsCollection() {
        User user = em.persist(minimalUser("vac-app@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "VacApp"));
        StaticGroup group = em.persist(minimalStatic("VacApp Group"));
        StaticVacancy vacancy = em.persist(minimalVacancy(group, Role.DPS));
        em.persist(minimalApplication(vacancy, character));
        em.flush();
        em.clear();

        StaticVacancy found = em.find(StaticVacancy.class, vacancy.getId());

        assertThat(found.getApplications()).hasSize(1);
    }

    @Test
    void staticHasInvitationsCollection() {
        User user = em.persist(minimalUser("inv-rel@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "InvRel"));
        StaticGroup group = em.persist(minimalStatic("InvRel Group"));
        em.persist(minimalInvitation(group, character));
        em.flush();
        em.clear();

        StaticGroup found = em.find(StaticGroup.class, group.getId());

        assertThat(found.getInvitations()).hasSize(1);
    }

    // ── Helpers ───────────────────────────────────────────────

    private StaticGroup minimalStatic(String name) {
        StaticGroup group = new StaticGroup();
        group.setName(name);
        group.setStatus(StaticStatus.RECRUITING);
        return group;
    }

    private StaticMembership minimalMembership(StaticGroup group, GameCharacter character) {
        StaticMembership membership = new StaticMembership();
        membership.setStaticGroup(group);
        membership.setCharacter(character);
        membership.setJoinedAt(Instant.now());
        return membership;
    }

    private StaticVacancy minimalVacancy(StaticGroup group, Role role) {
        StaticVacancy vacancy = new StaticVacancy();
        vacancy.setStaticGroup(group);
        vacancy.setRoleNeeded(role);
        return vacancy;
    }

    private Application minimalApplication(StaticVacancy vacancy, GameCharacter character) {
        Application application = new Application();
        application.setVacancy(vacancy);
        application.setCharacter(character);
        application.setStatus(ApplicationStatus.PENDING);
        application.setAppliedAt(Instant.now());
        return application;
    }

    private Invitation minimalInvitation(StaticGroup group, GameCharacter character) {
        Invitation invitation = new Invitation();
        invitation.setStaticGroup(group);
        invitation.setCharacter(character);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setSentAt(Instant.now());
        return invitation;
    }
}
