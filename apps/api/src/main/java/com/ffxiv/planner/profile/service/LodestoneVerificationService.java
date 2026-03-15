package com.ffxiv.planner.profile.service;

import com.ffxiv.planner.auth.entity.GameCharacter;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.CharacterRepository;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.common.exception.ForbiddenException;
import com.ffxiv.planner.common.exception.ResourceNotFoundException;
import com.ffxiv.planner.profile.dto.VerificationStartResponse;
import com.ffxiv.planner.profile.event.CharacterVerifiedEvent;
import com.ffxiv.planner.profile.integration.LodestoneCharacterData;
import com.ffxiv.planner.profile.integration.LodestoneClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LodestoneVerificationService {

    private static final String CACHE_NAME = "lodestone-verify";
    private static final String LODESTONE_BASE_URL = "https://na.finalfantasyxiv.com/lodestone/character/";
    private static final String VERIFICATION_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final LodestoneClient lodestoneClient;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public LodestoneVerificationService(UserRepository userRepository,
                                        CharacterRepository characterRepository,
                                        LodestoneClient lodestoneClient,
                                        CacheManager cacheManager,
                                        ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
        this.lodestoneClient = lodestoneClient;
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Inicia o fluxo de verificação Lodestone.
     * Gera um código aleatório de 8 caracteres e armazena em cache.
     * O usuário deve adicionar esse código à bio do seu perfil no Lodestone.
     */
    public VerificationStartResponse startVerification(UUID userId, UUID characterId) {
        User user = findActiveUser(userId);
        GameCharacter character = findCharacterWithOwnershipCheck(user, characterId);

        String code = generateVerificationCode();
        String cacheKey = "verify:" + characterId;
        cacheManager.getCache(CACHE_NAME).put(cacheKey, code);

        String lodestoneUrl = LODESTONE_BASE_URL + character.getLodestoneId();
        String instructions = "Adicione o código \"" + code + "\" à bio do seu personagem no Lodestone: " + lodestoneUrl;

        return new VerificationStartResponse(code, lodestoneUrl, instructions);
    }

    /**
     * Confirma a verificação Lodestone.
     * Busca o perfil no XIVAPI e verifica se a bio contém o código esperado.
     * Se confirmado: marca {@code isVerified=true}, importa avatar, raça, FC e remove código do cache.
     */
    @Transactional
    public void confirmVerification(UUID userId, UUID characterId) {
        User user = findActiveUser(userId);
        GameCharacter character = findCharacterWithOwnershipCheck(user, characterId);

        String cacheKey = "verify:" + characterId;
        Cache.ValueWrapper cached = cacheManager.getCache(CACHE_NAME).get(cacheKey);
        if (cached == null) {
            throw new ResourceNotFoundException("Código de verificação não encontrado ou expirado");
        }
        String expectedCode = (String) cached.get();

        LodestoneCharacterData data = lodestoneClient.fetchCharacter(character.getLodestoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Personagem", characterId));

        String bio = data.bio() != null ? data.bio() : "";
        if (!bio.contains(expectedCode)) {
            throw new IllegalStateException("código de verificação não encontrado na bio do personagem");
        }

        // Importar dados do Lodestone
        character.setVerified(true);
        if (data.avatarUrl() != null) character.setAvatarUrl(data.avatarUrl());
        if (data.race() != null) character.setRace(data.race());
        if (data.gender() != null) character.setGender(data.gender());
        if (data.freeCompany() != null) character.setFreeCompany(data.freeCompany());
        characterRepository.save(character);

        // Limpar código do cache
        cacheManager.getCache(CACHE_NAME).evict(cacheKey);

        eventPublisher.publishEvent(new CharacterVerifiedEvent(characterId, userId));
    }

    // ── helpers ───────────────────────────────────────────────

    private String generateVerificationCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(VERIFICATION_CODE_CHARS.charAt(
                    secureRandom.nextInt(VERIFICATION_CODE_CHARS.length())));
        }
        return code.toString();
    }

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
}
