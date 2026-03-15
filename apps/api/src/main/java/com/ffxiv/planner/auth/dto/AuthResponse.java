package com.ffxiv.planner.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
