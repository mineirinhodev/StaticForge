package com.ffxiv.planner.premium.dto;

public record CheckoutSessionResponse(
        String sessionId,
        String checkoutUrl
) {}
