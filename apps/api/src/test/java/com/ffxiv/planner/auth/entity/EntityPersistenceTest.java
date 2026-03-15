package com.ffxiv.planner.auth.entity;

import com.ffxiv.planner.common.auditing.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("datajpa")
@Import(JpaAuditingConfig.class)
class EntityPersistenceTest {

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

    // ── AuditableEntity ──────────────────────────────────────

    @Test
    void userAuditFieldsArePopulatedOnPersist() {
        User user = minimalUser("audit@test.com");

        User saved = em.persistFlushFind(user);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    // ── User ─────────────────────────────────────────────────

    @Test
    void userCanBePersistedAndRetrieved() {
        User user = minimalUser("user@test.com");

        User saved = em.persistFlushFind(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("user@test.com");
        assertThat(saved.isPremium()).isFalse();
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getFailedLoginAttempts()).isZero();
    }

    @Test
    void userWithOAuthFieldsCanBePersisted() {
        User user = minimalUser("oauth@test.com");
        user.setOauthProvider("DISCORD");
        user.setOauthProviderId("discord-id-123");

        User saved = em.persistFlushFind(user);

        assertThat(saved.getOauthProvider()).isEqualTo("DISCORD");
        assertThat(saved.getOauthProviderId()).isEqualTo("discord-id-123");
    }

    @Test
    void userSoftDeleteFieldIsNullByDefault() {
        User user = minimalUser("soft@test.com");

        User saved = em.persistFlushFind(user);

        assertThat(saved.getDeletedAt()).isNull();
    }

    // ── Character ─────────────────────────────────────────────

    @Test
    void characterCanBePersistedWithUser() {
        User user = em.persist(minimalUser("char@test.com"));
        em.flush();

        com.ffxiv.planner.auth.entity.Character character = minimalCharacter(user, "Thancred");

        com.ffxiv.planner.auth.entity.Character saved = em.persistFlushFind(character);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Thancred");
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.isVerified()).isFalse();
        assertThat(saved.isMain()).isFalse();
        assertThat(saved.isLookingForStatic()).isFalse();
    }

    @Test
    void characterAuditFieldsArePopulated() {
        User user = em.persist(minimalUser("charaudit@test.com"));
        em.flush();

        com.ffxiv.planner.auth.entity.Character character = minimalCharacter(user, "Y'shtola");

        com.ffxiv.planner.auth.entity.Character saved = em.persistFlushFind(character);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    // ── CharacterJob ──────────────────────────────────────────

    @Test
    void characterJobCanBePersistedWithRoleEnum() {
        User user = em.persist(minimalUser("job@test.com"));
        com.ffxiv.planner.auth.entity.Character character =
                em.persist(minimalCharacter(user, "Alphinaud"));
        em.flush();

        CharacterJob job = new CharacterJob();
        job.setCharacter(character);
        job.setJobName("SCH");
        job.setRole(Role.HEALER);

        CharacterJob saved = em.persistFlushFind(job);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getJobName()).isEqualTo("SCH");
        assertThat(saved.getRole()).isEqualTo(Role.HEALER);
        assertThat(saved.isMain()).isFalse();
    }

    @Test
    void roleEnumIsPersistedAsString() {
        User user = em.persist(minimalUser("role@test.com"));
        com.ffxiv.planner.auth.entity.Character character =
                em.persist(minimalCharacter(user, "Alisaie"));
        em.flush();

        CharacterJob tank = new CharacterJob();
        tank.setCharacter(character);
        tank.setJobName("PLD");
        tank.setRole(Role.TANK);

        CharacterJob dps = new CharacterJob();
        dps.setCharacter(character);
        dps.setJobName("SAM");
        dps.setRole(Role.DPS);

        em.persist(tank);
        em.persist(dps);
        em.flush();

        assertThat(em.persistFlushFind(tank).getRole()).isEqualTo(Role.TANK);
        assertThat(em.persistFlushFind(dps).getRole()).isEqualTo(Role.DPS);
    }

    // ── RefreshToken ──────────────────────────────────────────

    @Test
    void refreshTokenCanBePersistedWithUser() {
        User user = em.persist(minimalUser("token@test.com"));
        em.flush();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("abc123hash");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setCreatedAt(Instant.now());

        RefreshToken saved = em.persistFlushFind(token);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTokenHash()).isEqualTo("abc123hash");
        assertThat(saved.getRevokedAt()).isNull();
        assertThat(saved.getReplacedBy()).isNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
    }

    // ── Relationships ─────────────────────────────────────────

    @Test
    void userHasCharactersCollection() {
        User user = em.persist(minimalUser("rel@test.com"));
        em.persist(minimalCharacter(user, "Zenos"));
        em.persist(minimalCharacter(user, "Emet-Selch"));
        em.flush();
        em.clear();

        User found = em.find(User.class, user.getId());

        assertThat(found.getCharacters()).hasSize(2);
    }

    @Test
    void userHasRefreshTokensCollection() {
        User user = em.persist(minimalUser("tokens@test.com"));
        RefreshToken t1 = new RefreshToken();
        t1.setUser(user);
        t1.setTokenHash("hash-one");
        t1.setExpiresAt(Instant.now().plusSeconds(3600));
        t1.setCreatedAt(Instant.now());
        em.persist(t1);
        em.flush();
        em.clear();

        User found = em.find(User.class, user.getId());

        assertThat(found.getRefreshTokens()).hasSize(1);
    }

    @Test
    void characterHasJobsCollection() {
        User user = em.persist(minimalUser("jobs@test.com"));
        com.ffxiv.planner.auth.entity.Character character =
                em.persist(minimalCharacter(user, "G'raha Tia"));
        CharacterJob job1 = new CharacterJob();
        job1.setCharacter(character);
        job1.setJobName("RDM");
        job1.setRole(Role.DPS);
        em.persist(job1);
        em.flush();
        em.clear();

        com.ffxiv.planner.auth.entity.Character found =
                em.find(com.ffxiv.planner.auth.entity.Character.class, character.getId());

        assertThat(found.getJobs()).hasSize(1);
    }

    // ── Helpers ───────────────────────────────────────────────

    private User minimalUser(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    private com.ffxiv.planner.auth.entity.Character minimalCharacter(User user, String name) {
        com.ffxiv.planner.auth.entity.Character character =
                new com.ffxiv.planner.auth.entity.Character();
        character.setUser(user);
        character.setName(name);
        character.setServer("Tonberry");
        character.setDataCenter("Elemental");
        return character;
    }
}
