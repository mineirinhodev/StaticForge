package com.ffxiv.planner.premium.dto;

import com.ffxiv.planner.premium.entity.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record CreateSubscriptionRequest(
        @NotNull
        SubscriptionPlan plan,
        String successUrl,
        String cancelUrl
) {}
