package com.ffxiv.planner.auth.repository;

import com.ffxiv.planner.auth.entity.*;
import com.ffxiv.planner.common.auditing.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
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
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("datajpa")
@Import(JpaAuditingConfig.class)
class RepositoryQueryTest {

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

    @Autowired TestEntityManager em;
    @Autowired UserRepository userRepository;
    @Autowired CharacterRepository characterRepository;
    @Autowired CharacterJobRepository characterJobRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    // ── UserRepository ───────────────────────────────────────

    @Test
    void findByEmail_returnsUser() {
        em.persistFlushFind(minimalUser("find@test.com"));

        Optional<User> result = userRepository.findByEmail("find@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("find@test.com");
    }

    @Test
    void findByEmail_returnsEmpty_whenNotFound() {
        assertThat(userRepository.findByEmail("ghost@test.com")).isEmpty();
    }

    @Test
    void existsByEmail_returnsTrue_whenExists() {
        em.persistFlushFind(minimalUser("exists@test.com"));

        assertThat(userRepository.existsByEmail("exists@test.com")).isTrue();
    }

    @Test
    void existsByEmail_returnsFalse_whenNotExists() {
        assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
    }

    @Test
    void findByOAuthProvider_returnsUser() {
        User user = minimalUser("oauth@test.com");
        user.setOauthProvider("DISCORD");
        user.setOauthProviderId("disc-999");
        em.persistFlushFind(user);

        Optional<User> result = userRepository.findByOAuthProvider("DISCORD", "disc-999");

        assertThat(result).isPresent();
        assertThat(result.get().getOauthProviderId()).isEqualTo("disc-999");
    }

    @Test
    void findActiveById_returnsUser_whenNotDeleted() {
        User user = em.persistFlushFind(minimalUser("active@test.com"));

        Optional<User> result = userRepository.findActiveById(user.getId());

        assertThat(result).isPresent();
    }

    @Test
    void findActiveById_returnsEmpty_whenSoftDeleted() {
        User user = minimalUser("deleted@test.com");
        user.setDeletedAt(Instant.now());
        User saved = em.persistFlushFind(user);

        Optional<User> result = userRepository.findActiveById(saved.getId());

        assertThat(result).isEmpty();
    }

    // ── CharacterRepository ──────────────────────────────────

    @Test
    void findByUserIdOrderByIsMainDesc_returnsCharacters() {
        User user = em.persist(minimalUser("charlist@test.com"));
        GameCharacter main = minimalCharacter(user, "Thancred");
        main.setMain(true);
        em.persist(main);
        em.persist(minimalCharacter(user, "Y'shtola"));
        em.flush();

        List<GameCharacter> result = characterRepository.findByUserIdOrderByIsMainDesc(user.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isMain()).isTrue();
    }

    @Test
    void findByLodestoneId_returnsCharacter() {
        User user = em.persist(minimalUser("lodestone@test.com"));
        GameCharacter character = minimalCharacter(user, "Alphinaud");
        character.setLodestoneId(12345678L);
        em.persistFlushFind(character);

        Optional<GameCharacter> result = characterRepository.findByLodestoneId(12345678L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alphinaud");
    }

    @Test
    void existsByLodestoneId_returnsTrue_whenExists() {
        User user = em.persist(minimalUser("lodexists@test.com"));
        GameCharacter character = minimalCharacter(user, "Alisaie");
        character.setLodestoneId(99999L);
        em.persistFlushFind(character);

        assertThat(characterRepository.existsByLodestoneId(99999L)).isTrue();
    }

    @Test
    void findMainByUserId_returnsMainCharacter() {
        User user = em.persist(minimalUser("main@test.com"));
        GameCharacter main = minimalCharacter(user, "G'raha Tia");
        main.setMain(true);
        em.persist(main);
        em.persist(minimalCharacter(user, "Zenos"));
        em.flush();

        Optional<GameCharacter> result = characterRepository.findMainByUserId(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("G'raha Tia");
    }

    @Test
    void findLFSByDataCenter_returnsOnlyVerifiedLFSCharacters() {
        User user = em.persist(minimalUser("lfs@test.com"));

        GameCharacter lfs = minimalCharacter(user, "Emet-Selch");
        lfs.setLookingForStatic(true);
        lfs.setVerified(true);
        em.persist(lfs);

        GameCharacter notLfs = minimalCharacter(user, "Venat");
        notLfs.setLookingForStatic(false);
        em.persist(notLfs);

        GameCharacter notVerified = minimalCharacter(user, "Hades");
        notVerified.setLookingForStatic(true);
        notVerified.setVerified(false);
        em.persist(notVerified);

        em.flush();

        Page<GameCharacter> result = characterRepository.findLFSByDataCenter(
                "Elemental", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Emet-Selch");
    }

    // ── CharacterJobRepository ───────────────────────────────

    @Test
    void findByCharacterId_returnsAllJobs() {
        User user = em.persist(minimalUser("jobs@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Zenos"));
        em.persist(minimalCharacterJob(character, "WAR", Role.TANK));
        em.persist(minimalCharacterJob(character, "GNB", Role.TANK));
        em.flush();

        List<CharacterJob> result = characterJobRepository.findByCharacterId(character.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void findMainByCharacterId_returnsMainJob() {
        User user = em.persist(minimalUser("mainjob@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Alisaie"));
        CharacterJob main = minimalCharacterJob(character, "RDM", Role.DPS);
        main.setMain(true);
        em.persist(main);
        em.persist(minimalCharacterJob(character, "BLM", Role.DPS));
        em.flush();

        Optional<CharacterJob> result = characterJobRepository.findMainByCharacterId(character.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getJobName()).isEqualTo("RDM");
    }

    @Test
    void existsByCharacterIdAndJobName_returnsTrue_whenExists() {
        User user = em.persist(minimalUser("jobexists@test.com"));
        GameCharacter character = em.persist(minimalCharacter(user, "Estinien"));
        em.persist(minimalCharacterJob(character, "DRG", Role.DPS));
        em.flush();

        assertThat(characterJobRepository.existsByCharacterIdAndJobName(
                character.getId(), "DRG")).isTrue();
    }

    // ── RefreshTokenRepository ───────────────────────────────

    @Test
    void findByTokenHashAndRevokedAtIsNull_returnsActiveToken() {
        User user = em.persist(minimalUser("token@test.com"));
        em.persist(minimalRefreshToken(user, "active-hash"));
        em.flush();

        Optional<RefreshToken> result =
                refreshTokenRepository.findByTokenHashAndRevokedAtIsNull("active-hash");

        assertThat(result).isPresent();
    }

    @Test
    void findByTokenHashAndRevokedAtIsNull_returnsEmpty_whenRevoked() {
        User user = em.persist(minimalUser("revoked@test.com"));
        RefreshToken token = minimalRefreshToken(user, "revoked-hash");
        token.setRevokedAt(Instant.now());
        em.persist(token);
        em.flush();

        Optional<RefreshToken> result =
                refreshTokenRepository.findByTokenHashAndRevokedAtIsNull("revoked-hash");

        assertThat(result).isEmpty();
    }

    @Test
    void revokeAllByUserId_setsRevokedAtOnActiveTokens() {
        User user = em.persist(minimalUser("revokeall@test.com"));
        em.persist(minimalRefreshToken(user, "hash-a"));
        em.persist(minimalRefreshToken(user, "hash-b"));
        em.flush();

        refreshTokenRepository.revokeAllByUserId(user.getId());
        em.flush();
        em.clear();

        List<RefreshToken> tokens = refreshTokenRepository.findAll()
                .stream().filter(t -> t.getUser().getId().equals(user.getId())).toList();
        assertThat(tokens).allMatch(t -> t.getRevokedAt() != null);
        assertThat(tokens).hasSize(2); // records still exist, just revoked
    }

    @Test
    void deleteExpired_removesOnlyExpiredTokens() {
        User user = em.persist(minimalUser("expire@test.com"));

        RefreshToken expired = minimalRefreshToken(user, "expired-hash");
        expired.setExpiresAt(Instant.now().minusSeconds(3600));
        em.persist(expired);

        em.persist(minimalRefreshToken(user, "valid-hash")); // expires in future
        em.flush();

        refreshTokenRepository.deleteExpired();
        em.flush();
        em.clear();

        assertThat(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull("expired-hash")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull("valid-hash")).isPresent();
    }

    // ── Helpers ───────────────────────────────────────────────

    private User minimalUser(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    private GameCharacter minimalCharacter(User user, String name) {
        GameCharacter character = new GameCharacter();
        character.setUser(user);
        character.setName(name);
        character.setServer("Tonberry");
        character.setDataCenter("Elemental");
        return character;
    }

    private CharacterJob minimalCharacterJob(GameCharacter character, String jobName, Role role) {
        CharacterJob job = new CharacterJob();
        job.setCharacter(character);
        job.setJobName(jobName);
        job.setRole(role);
        return job;
    }

    private RefreshToken minimalRefreshToken(User user, String hash) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setCreatedAt(Instant.now());
        return token;
    }
}
