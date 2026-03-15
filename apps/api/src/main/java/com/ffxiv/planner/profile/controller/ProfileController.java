package com.ffxiv.planner.profile.controller;

import com.ffxiv.planner.profile.dto.ProfileResponse;
import com.ffxiv.planner.profile.dto.ProfileUpdateRequest;
import com.ffxiv.planner.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** GET /profile/me — perfil completo do usuário autenticado. */
    @GetMapping("/profile/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(profileService.getMyProfile(userId));
    }

    /** PUT /profile/me — atualizar campos do perfil. */
    @PutMapping("/profile/me")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    /** GET /users/{id}/profile — perfil público de outro usuário. */
    @GetMapping("/users/{id}/profile")
    public ResponseEntity<ProfileResponse> getPublicProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(profileService.getPublicProfile(id));
    }
}
