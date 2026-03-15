package com.ffxiv.planner.premium.event;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionCanceledEvent(
        UUID userId,
        LocalDate endDate
) {}
