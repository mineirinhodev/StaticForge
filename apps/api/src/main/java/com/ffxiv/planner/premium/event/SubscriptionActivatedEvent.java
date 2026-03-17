package com.ffxiv.planner.premium.event;

import com.ffxiv.planner.premium.entity.SubscriptionPlan;

import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionActivatedEvent(
        UUID userId,
        SubscriptionPlan plan,
        LocalDate endDate
) {}
