package com.ffxiv.planner.auth.entity;

import com.ffxiv.planner.auth.DataJpaTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class EntityPersistenceTest extends DataJpaTestBase {

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
        User saved = em.persistFlushFind(minimalUser("audit@test.com"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    // ── User ─────────────────────────────────────────────────

    @Test
    void userCanBePersistedAndRetrieved() {
        User saved = em.persistFlushFind(minimalUser("user@test.com"));

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
        User saved = em.persistFlushFind(minimalUser("soft@test.com"));

        assertThat(saved.getDeletedAt()).isNull();
    }

    // ── GameCharacter ─────────────────────────────────────────

    @Test
    void characterCanBePersistedWithUser() {
        User user = em.persist(minimalUser("char@test.com"));
        GameCharacter saved = em.persistFlushFind(minimalCharacter(user, "Thancred"));

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
        GameCharacter saved = em.persistFlushFind(minimalCharacter(user, "Y'shtola"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    // ── CharacterJob ──────────────────────────────────────────

    @Test
    void characterJobCanBePersistedWithRoleEnum() {
        User user = em.persist(minimalUser("job@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Alphinaud"));

        CharacterJob saved = em.persistFlushFind(minimalCharacterJob(character, "SCH", Role.HEALER));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getJobName()).isEqualTo("SCH");
        assertThat(saved.getRole()).isEqualTo(Role.HEALER);
        assertThat(saved.isMain()).isFalse();
    }

    @Test
    void roleEnumIsPersistedAsString() {
        User user = em.persist(minimalUser("role@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Alisaie"));

        CharacterJob tank = em.persist(minimalCharacterJob(character, "PLD", Role.TANK));
        CharacterJob dps = em.persist(minimalCharacterJob(character, "SAM", Role.DPS));
        em.flush();
        em.clear();

        assertThat(em.find(CharacterJob.class, tank.getId()).getRole()).isEqualTo(Role.TANK);
        assertThat(em.find(CharacterJob.class, dps.getId()).getRole()).isEqualTo(Role.DPS);
    }

    // ── RefreshToken ──────────────────────────────────────────

    @Test
    void refreshTokenCanBePersistedWithUser() {
        User user = em.persist(minimalUser("token@test.com"));

        RefreshToken saved = em.persistFlushFind(minimalRefreshToken(user, "abc123hash"));

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
        em.persist(minimalRefreshToken(user, "hash-one"));
        em.flush();
        em.clear();

        User found = em.find(User.class, user.getId());

        assertThat(found.getRefreshTokens()).hasSize(1);
    }

    @Test
    void characterHasJobsCollection() {
        User user = em.persist(minimalUser("jobs@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "G'raha Tia"));
        em.persist(minimalCharacterJob(character, "RDM", Role.DPS));
        em.flush();
        em.clear();

        GameCharacter found = em.find(GameCharacter.class, character.getId());

        assertThat(found.getJobs()).hasSize(1);
    }
}
