package com.ffxiv.planner.premium.dto;

import com.ffxiv.planner.premium.entity.SubscriptionPlan;
import com.ffxiv.planner.premium.entity.SubscriptionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate endDate,
        boolean cancelAtPeriodEnd,
        boolean autoRenew,
        boolean trialUsed,
        Instant createdAt
) {}
