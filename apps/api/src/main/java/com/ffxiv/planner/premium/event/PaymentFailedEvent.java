package com.ffxiv.planner.premium.event;

import java.util.UUID;

public record PaymentFailedEvent(
        UUID userId,
        String failureReason
) {}
