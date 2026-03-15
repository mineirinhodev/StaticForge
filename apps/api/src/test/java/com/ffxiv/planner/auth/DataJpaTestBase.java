package com.ffxiv.planner.auth;

import com.ffxiv.planner.auth.entity.*;
import com.ffxiv.planner.common.auditing.JpaAuditingConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("datajpa")
@Import(JpaAuditingConfig.class)
public abstract class DataJpaTestBase {

    // ── Helpers ───────────────────────────────────────────────

    protected User minimalUser(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    protected GameCharacter minimalCharacter(User user, String name) {
        GameCharacter character = new GameCharacter();
        character.setUser(user);
        character.setName(name);
        character.setServer("Tonberry");
        character.setDataCenter("Elemental");
        return character;
    }

    protected CharacterJob minimalCharacterJob(GameCharacter character, String jobName, Role role) {
        CharacterJob job = new CharacterJob();
        job.setCharacter(character);
        job.setJobName(jobName);
        job.setRole(role);
        return job;
    }

    protected RefreshToken minimalRefreshToken(User user, String hash) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setCreatedAt(Instant.now());
        return token;
    }
}
