package com.ffxiv.planner.profile;

import com.ffxiv.planner.auth.entity.GameCharacter;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.CharacterRepository;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.profile.dto.VerificationStartResponse;
import com.ffxiv.planner.profile.integration.LodestoneCharacterData;
import com.ffxiv.planner.profile.integration.LodestoneClient;
import com.ffxiv.planner.profile.service.LodestoneVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LodestoneVerificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private LodestoneClient lodestoneClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CacheManager cacheManager;
    private LodestoneVerificationService verificationService;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager("lodestone-verify");
        verificationService = new LodestoneVerificationService(
                userRepository, characterRepository, lodestoneClient, cacheManager, eventPublisher);
    }

    // ── startVerification ─────────────────────────────────────

    @Test
    void startVerification_returnsVerificationCodeAndLodestoneUrl() {
        User user = basicUser();
        GameCharacter character = character(user, 12345L);
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(character.getId())).thenReturn(Optional.of(character));

        VerificationStartResponse response = verificationService.startVerification(user.getId(), character.getId());

        assertThat(response.verificationCode()).hasSize(8);
        assertThat(response.lodestoneUrl()).contains("12345");
        assertThat(response.instructions()).isNotBlank();
    }

    @Test
    void startVerification_storesCodeInCache() {
        User user = basicUser();
        GameCharacter character = character(user, 12345L);
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(character.getId())).thenReturn(Optional.of(character));

        verificationService.startVerification(user.getId(), character.getId());

        String cacheKey = "verify:" + character.getId();
        assertThat(cacheManager.getCache("lodestone-verify").get(cacheKey)).isNotNull();
    }

    @Test
    void startVerification_throwsWhenCharacterBelongsToAnotherUser() {
        User user = basicUser();
        User other = basicUser();
        GameCharacter foreignChar = character(other, 99999L);
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(foreignChar.getId())).thenReturn(Optional.of(foreignChar));

        assertThatThrownBy(() -> verificationService.startVerification(user.getId(), foreignChar.getId()))
                .isInstanceOf(com.ffxiv.planner.common.exception.ForbiddenException.class);
    }

    // ── confirmVerification ───────────────────────────────────

    @Test
    void confirmVerification_marksCharacterAsVerifiedWhenCodeMatches() {
        User user = basicUser();
        GameCharacter character = character(user, 12345L);
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(character.getId())).thenReturn(Optional.of(character));

        // Pré-popular cache com código
        String code = "ABCD1234";
        cacheManager.getCache("lodestone-verify").put("verify:" + character.getId(), code);

        // XIVAPI retorna bio com o código
        LodestoneCharacterData data = new LodestoneCharacterData(12345L, "Luna", "Behemoth",
                "Bio com código ABCD1234", "https://img.example.com/avatar.jpg", "Hyur", "Female", "Example FC");
        when(lodestoneClient.fetchCharacter(12345L)).thenReturn(Optional.of(data));
        when(characterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        verificationService.confirmVerification(user.getId(), character.getId());

        assertThat(character.isVerified()).isTrue();
        assertThat(character.getAvatarUrl()).isEqualTo("https://img.example.com/avatar.jpg");
        verify(eventPublisher).publishEvent(any(com.ffxiv.planner.profile.event.CharacterVerifiedEvent.class));
    }

    @Test
    void confirmVerification_throwsWhenCodeNotFound() {
        User user = basicUser();
        GameCharacter character = character(user, 12345L);
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(character.getId())).thenReturn(Optional.of(character));
        // cache está vazio — código expirou ou startVerification não foi chamado

        assertThatThrownBy(() -> verificationService.confirmVerification(user.getId(), character.getId()))
                .isInstanceOf(com.ffxiv.planner.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("verificação");
    }

    @Test
    void confirmVerification_throwsWhenCodeNotPresentInBio() {
        User user = basicUser();
        GameCharacter character = character(user, 12345L);
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(character.getId())).thenReturn(Optional.of(character));

        cacheManager.getCache("lodestone-verify").put("verify:" + character.getId(), "ABCD1234");

        LodestoneCharacterData data = new LodestoneCharacterData(12345L, "Luna", "Behemoth",
                "Bio sem o código", null, null, null, null);
        when(lodestoneClient.fetchCharacter(12345L)).thenReturn(Optional.of(data));

        assertThatThrownBy(() -> verificationService.confirmVerification(user.getId(), character.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("código");
    }

    // ── helpers ───────────────────────────────────────────────

    private User basicUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("user@test.com");
        u.setActive(true);
        u.setCharacters(new ArrayList<>());
        return u;
    }

    private GameCharacter character(User user, Long lodestoneId) {
        GameCharacter c = new GameCharacter();
        c.setId(UUID.randomUUID());
        c.setUser(user);
        c.setName("Luna");
        c.setServer("Behemoth");
        c.setDataCenter("Primal");
        c.setLodestoneId(lodestoneId);
        c.setJobs(new ArrayList<>());
        user.getCharacters().add(c);
        return c;
    }
}
