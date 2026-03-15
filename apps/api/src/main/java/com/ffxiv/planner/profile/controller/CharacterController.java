package com.ffxiv.planner.profile.controller;

import com.ffxiv.planner.profile.dto.CharacterCreateRequest;
import com.ffxiv.planner.profile.dto.CharacterResponse;
import com.ffxiv.planner.profile.service.CharacterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/characters")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    /** GET /characters — lista personagens do usuário autenticado. */
    @GetMapping
    public ResponseEntity<List<CharacterResponse>> listCharacters(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(characterService.listCharacters(userId));
    }

    /** POST /characters — adiciona novo personagem. */
    @PostMapping
    public ResponseEntity<CharacterResponse> createCharacter(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CharacterCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(characterService.createCharacter(userId, request));
    }

    /** PUT /characters/{id}/main — promove personagem a main. */
    @PutMapping("/{id}/main")
    public ResponseEntity<CharacterResponse> setMainCharacter(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(characterService.setMainCharacter(userId, id));
    }

    /** DELETE /characters/{id} — remove personagem. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCharacter(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        characterService.deleteCharacter(userId, id);
        return ResponseEntity.noContent().build();
    }
}
