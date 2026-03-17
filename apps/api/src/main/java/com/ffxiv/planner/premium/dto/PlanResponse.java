package com.ffxiv.planner.premium.dto;

import com.ffxiv.planner.premium.entity.SubscriptionPlan;

import java.math.BigDecimal;
import java.util.List;

public record PlanResponse(
        String planId,
        SubscriptionPlan plan,
        String name,
        BigDecimal price,
        String currency,
        String interval,
        BigDecimal savingsPercent,
        List<String> features
) {}
