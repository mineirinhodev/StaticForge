package com.ffxiv.planner.profile.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Optional;

/**
 * Cliente HTTP para o XIVAPI — busca perfis de personagens do Lodestone.
 *
 * <p>Endpoint usado: {@code GET /character/{id}?data=CJ}
 * (CJ = Character + Jobs, sem dados desnecessários)</p>
 */
@Component
public class LodestoneClient {

    private final WebClient webClient;

    public LodestoneClient(WebClient.Builder webClientBuilder,
                           @Value("${app.xivapi.base-url:https://xivapi.com}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Busca dados do personagem no Lodestone via XIVAPI.
     *
     * @return dados do personagem, ou {@link Optional#empty()} se não encontrado
     */
    @SuppressWarnings("unchecked")
    public Optional<LodestoneCharacterData> fetchCharacter(Long lodestoneId) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/character/{id}", lodestoneId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return Optional.empty();

            Map<String, Object> character = (Map<String, Object>) response.get("Character");
            if (character == null) return Optional.empty();

            return Optional.of(new LodestoneCharacterData(
                    lodestoneId,
                    (String) character.get("Name"),
                    (String) character.get("Server"),
                    (String) character.get("Bio"),
                    (String) character.get("Avatar"),
                    (String) character.get("Race"),
                    (String) character.get("Gender"),
                    extractFreeCompany(character)
            ));
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        }
    }

    private String extractFreeCompany(Map<String, Object> character) {
        Object fc = character.get("FreeCompanyName");
        return fc != null ? fc.toString() : null;
    }
}
