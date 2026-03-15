package com.ffxiv.planner.profile.service;

import com.ffxiv.planner.auth.entity.GameCharacter;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.CharacterRepository;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.common.exception.ForbiddenException;
import com.ffxiv.planner.common.exception.ResourceNotFoundException;
import com.ffxiv.planner.profile.dto.CharacterCreateRequest;
import com.ffxiv.planner.profile.dto.CharacterResponse;
import com.ffxiv.planner.profile.event.CharacterCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CharacterService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CharacterService(UserRepository userRepository,
                            CharacterRepository characterRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Lista personagens do usuário, main primeiro.
     */
    public List<CharacterResponse> listCharacters(UUID userId) {
        return characterRepository.findByUserIdOrderByIsMainDesc(userId).stream()
                .map(CharacterResponse::from)
                .toList();
    }

    /**
     * Adiciona um novo personagem ao usuário.
     * O primeiro personagem torna-se main automaticamente.
     * Se {@code isMain=true} no request, desmarca o main atual primeiro.
     */
    @Transactional
    public CharacterResponse createCharacter(UUID userId, CharacterCreateRequest request) {
        User user = findActiveUser(userId);

        boolean firstCharacter = user.getCharacters().isEmpty();
        boolean shouldBeMain = firstCharacter || request.isMain();

        if (shouldBeMain) {
            demoteCurrentMain(user);
        }

        GameCharacter character = new GameCharacter();
        character.setUser(user);
        character.setName(request.name());
        character.setServer(request.server());
        character.setDataCenter(request.dataCenter());
        character.setMain(shouldBeMain);
        character.setLookingForStatic(request.lookingForStatic());
        character.setAvailability(request.availability());

        GameCharacter saved = characterRepository.save(character);
        eventPublisher.publishEvent(new CharacterCreatedEvent(saved.getId(), userId));

        return CharacterResponse.from(saved);
    }

    /**
     * Promove o personagem indicado a main, demovendo o atual.
     */
    @Transactional
    public CharacterResponse setMainCharacter(UUID userId, UUID characterId) {
        User user = findActiveUser(userId);
        GameCharacter target = findCharacterWithOwnershipCheck(user, characterId);

        demoteCurrentMain(user);
        target.setMain(true);
        characterRepository.save(target);

        return CharacterResponse.from(target);
    }

    /**
     * Remove um personagem.
     * Lança {@link IllegalStateException} se for o único.
     * Se era main, promove outro automaticamente.
     */
    @Transactional
    public void deleteCharacter(UUID userId, UUID characterId) {
        User user = findActiveUser(userId);
        GameCharacter target = findCharacterWithOwnershipCheck(user, characterId);

        if (user.getCharacters().size() == 1) {
            throw new IllegalStateException("Não é possível remover o único personagem da conta");
        }

        if (target.isMain()) {
            user.getCharacters().stream()
                    .filter(c -> !c.getId().equals(characterId))
                    .findFirst()
                    .ifPresent(c -> {
                        c.setMain(true);
                        characterRepository.save(c);
                    });
        }

        characterRepository.delete(target);
    }

    // ── helpers ───────────────────────────────────────────────

    private User findActiveUser(UUID userId) {
        return userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private GameCharacter findCharacterWithOwnershipCheck(User user, UUID characterId) {
        GameCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character", characterId));
        if (!character.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException();
        }
        return character;
    }

    private void demoteCurrentMain(User user) {
        user.getCharacters().stream()
                .filter(GameCharacter::isMain)
                .findFirst()
                .ifPresent(c -> {
                    c.setMain(false);
                    characterRepository.save(c);
                });
    }
}
