package com.ffxiv.planner.profile.dto;

public record VerificationStartResponse(
        String verificationCode,
        String lodestoneUrl,
        String instructions
) {}
