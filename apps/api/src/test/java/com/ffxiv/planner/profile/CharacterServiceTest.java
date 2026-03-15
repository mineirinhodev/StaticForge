package com.ffxiv.planner.profile;

import com.ffxiv.planner.auth.entity.GameCharacter;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.CharacterRepository;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.profile.dto.CharacterCreateRequest;
import com.ffxiv.planner.profile.dto.CharacterResponse;
import com.ffxiv.planner.profile.service.CharacterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CharacterService characterService;

    @BeforeEach
    void setUp() {
        characterService = new CharacterService(userRepository, characterRepository, eventPublisher);
    }

    // ── createCharacter ───────────────────────────────────────

    @Test
    void createCharacter_firstCharacterBecomesMain() {
        User user = emptyUser();
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.save(any())).thenAnswer(inv -> {
            GameCharacter c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CharacterCreateRequest req = new CharacterCreateRequest("Luna", "Behemoth", "Primal", false, false, null);
        CharacterResponse result = characterService.createCharacter(user.getId(), req);

        assertThat(result.isMain()).isTrue();
    }

    @Test
    void createCharacter_setsMainAndUnmarksCurrentMain() {
        User user = emptyUser();
        GameCharacter existing = character(user, "Old Main", true);
        user.getCharacters().add(existing);

        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.save(any())).thenAnswer(inv -> {
            GameCharacter c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });

        CharacterCreateRequest req = new CharacterCreateRequest("New Main", "Behemoth", "Primal", true, false, null);
        characterService.createCharacter(user.getId(), req);

        // existing main deve ter sido salvo com isMain=false
        ArgumentCaptor<GameCharacter> captor = ArgumentCaptor.forClass(GameCharacter.class);
        verify(characterRepository, atLeast(2)).save(captor.capture());
        boolean oldMainWasDemoted = captor.getAllValues().stream()
                .anyMatch(c -> "Old Main".equals(c.getName()) && !c.isMain());
        assertThat(oldMainWasDemoted).isTrue();
    }

    @Test
    void createCharacter_publishesCharacterCreatedEvent() {
        User user = emptyUser();
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.save(any())).thenAnswer(inv -> {
            GameCharacter c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        characterService.createCharacter(user.getId(),
                new CharacterCreateRequest("Luna", "Behemoth", "Primal", false, false, null));

        verify(eventPublisher).publishEvent(any(com.ffxiv.planner.profile.event.CharacterCreatedEvent.class));
    }

    @Test
    void createCharacter_throwsWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterService.createCharacter(id,
                new CharacterCreateRequest("Luna", "Behemoth", "Primal", false, false, null)))
                .isInstanceOf(com.ffxiv.planner.common.exception.ResourceNotFoundException.class);
    }

    // ── listCharacters ────────────────────────────────────────

    @Test
    void listCharacters_returnsCharactersOrderedByMainFirst() {
        User user = emptyUser();
        GameCharacter alt = character(user, "Alt", false);
        GameCharacter main = character(user, "Main", true);
        when(characterRepository.findByUserIdOrderByIsMainDesc(user.getId()))
                .thenReturn(List.of(main, alt));

        List<CharacterResponse> result = characterService.listCharacters(user.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isMain()).isTrue();
    }

    // ── setMainCharacter ──────────────────────────────────────

    @Test
    void setMainCharacter_demotesCurrentMainAndPromotesTarget() {
        User user = emptyUser();
        GameCharacter currentMain = character(user, "Current Main", true);
        GameCharacter toPromote = character(user, "New Main", false);
        user.getCharacters().addAll(List.of(currentMain, toPromote));

        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(toPromote.getId())).thenReturn(Optional.of(toPromote));
        when(characterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        characterService.setMainCharacter(user.getId(), toPromote.getId());

        assertThat(toPromote.isMain()).isTrue();
        assertThat(currentMain.isMain()).isFalse();
    }

    @Test
    void setMainCharacter_throwsWhenCharacterBelongsToAnotherUser() {
        User user = emptyUser();
        User otherUser = emptyUser();
        GameCharacter foreignChar = character(otherUser, "Foreign", false);

        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(foreignChar.getId())).thenReturn(Optional.of(foreignChar));

        assertThatThrownBy(() -> characterService.setMainCharacter(user.getId(), foreignChar.getId()))
                .isInstanceOf(com.ffxiv.planner.common.exception.ForbiddenException.class);
    }

    // ── deleteCharacter ───────────────────────────────────────

    @Test
    void deleteCharacter_removesCharacter() {
        User user = emptyUser();
        GameCharacter c1 = character(user, "Main", true);
        GameCharacter c2 = character(user, "Alt", false);
        user.getCharacters().addAll(List.of(c1, c2));

        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(c2.getId())).thenReturn(Optional.of(c2));

        characterService.deleteCharacter(user.getId(), c2.getId());

        verify(characterRepository).delete(c2);
    }

    @Test
    void deleteCharacter_promotesAnotherMainWhenMainIsDeleted() {
        User user = emptyUser();
        GameCharacter main = character(user, "Main", true);
        GameCharacter alt = character(user, "Alt", false);
        user.getCharacters().addAll(List.of(main, alt));

        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(main.getId())).thenReturn(Optional.of(main));
        when(characterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        characterService.deleteCharacter(user.getId(), main.getId());

        assertThat(alt.isMain()).isTrue();
        verify(characterRepository).delete(main);
    }

    @Test
    void deleteCharacter_throwsWhenOnlyOneCharacter() {
        User user = emptyUser();
        GameCharacter only = character(user, "Only", true);
        user.getCharacters().add(only);

        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(only.getId())).thenReturn(Optional.of(only));

        assertThatThrownBy(() -> characterService.deleteCharacter(user.getId(), only.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único");
    }

    @Test
    void deleteCharacter_throwsWhenCharacterBelongsToAnotherUser() {
        User user = emptyUser();
        User other = emptyUser();
        GameCharacter foreign = character(other, "Foreign", false);

        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(characterRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> characterService.deleteCharacter(user.getId(), foreign.getId()))
                .isInstanceOf(com.ffxiv.planner.common.exception.ForbiddenException.class);
    }

    // ── helpers ───────────────────────────────────────────────

    private User emptyUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("user@test.com");
        u.setActive(true);
        u.setCharacters(new ArrayList<>());
        return u;
    }

    private GameCharacter character(User user, String name, boolean isMain) {
        GameCharacter c = new GameCharacter();
        c.setId(UUID.randomUUID());
        c.setUser(user);
        c.setName(name);
        c.setServer("Behemoth");
        c.setDataCenter("Primal");
        c.setMain(isMain);
        c.setJobs(new ArrayList<>());
        return c;
    }
}
