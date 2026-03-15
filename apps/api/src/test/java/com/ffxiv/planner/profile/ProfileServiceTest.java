package com.ffxiv.planner.profile;

import com.ffxiv.planner.auth.entity.CharacterJob;
import com.ffxiv.planner.auth.entity.GameCharacter;
import com.ffxiv.planner.auth.entity.Role;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.RefreshTokenRepository;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.profile.dto.ProfileResponse;
import com.ffxiv.planner.profile.dto.ProfileUpdateRequest;
import com.ffxiv.planner.profile.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userRepository, refreshTokenRepository, eventPublisher);
    }

    // ── getMyProfile ──────────────────────────────────────────

    @Test
    void getMyProfile_returnsProfileWithCharactersAndJobs() {
        User user = userWithCharacter();
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));

        ProfileResponse profile = profileService.getMyProfile(user.getId());

        assertThat(profile.id()).isEqualTo(user.getId());
        assertThat(profile.email()).isEqualTo(user.getEmail());
        assertThat(profile.characters()).hasSize(1);
        assertThat(profile.characters().get(0).jobs()).hasSize(1);
    }

    @Test
    void getMyProfile_throwsWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getMyProfile(id))
                .isInstanceOf(com.ffxiv.planner.common.exception.ResourceNotFoundException.class);
    }

    // ── getPublicProfile ──────────────────────────────────────

    @Test
    void getPublicProfile_omitsEmail() {
        User user = userWithCharacter();
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));

        ProfileResponse profile = profileService.getPublicProfile(user.getId());

        assertThat(profile.email()).isNull();
        assertThat(profile.characters()).isNotEmpty();
    }

    @Test
    void getPublicProfile_throwsWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getPublicProfile(id))
                .isInstanceOf(com.ffxiv.planner.common.exception.ResourceNotFoundException.class);
    }

    // ── updateProfile ─────────────────────────────────────────

    @Test
    void updateProfile_updatesLocaleAndBio() {
        User user = basicUser();
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProfileUpdateRequest request = new ProfileUpdateRequest("pt-BR", "Nova bio", null);
        ProfileResponse result = profileService.updateProfile(user.getId(), request);

        assertThat(result.locale()).isEqualTo("pt-BR");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_publishesProfileUpdatedEvent() {
        User user = basicUser();
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        profileService.updateProfile(user.getId(), new ProfileUpdateRequest("en", null, null));

        verify(eventPublisher).publishEvent(any(com.ffxiv.planner.profile.event.ProfileUpdatedEvent.class));
    }

    @Test
    void updateProfile_ignoresNullFields() {
        User user = basicUser();
        user.setLocale("en");
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // só bio é fornecida
        profileService.updateProfile(user.getId(), new ProfileUpdateRequest(null, "bio", null));

        verify(userRepository).save(argThat(u -> "en".equals(u.getLocale())));
    }

    // ── helpers ───────────────────────────────────────────────

    private User basicUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("user@test.com");
        u.setActive(true);
        return u;
    }

    private User userWithCharacter() {
        User u = basicUser();
        GameCharacter c = new GameCharacter();
        c.setId(UUID.randomUUID());
        c.setUser(u);
        c.setName("Luna");
        c.setServer("Behemoth");
        c.setDataCenter("Primal");
        c.setMain(true);
        CharacterJob job = new CharacterJob();
        job.setId(UUID.randomUUID());
        job.setCharacter(c);
        job.setJobName("WAR");
        job.setRole(Role.TANK);
        c.getJobs().add(job);
        u.getCharacters().add(c);
        return u;
    }
}
