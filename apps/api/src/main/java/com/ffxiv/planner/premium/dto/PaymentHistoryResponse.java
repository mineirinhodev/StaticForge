package com.ffxiv.planner.premium.dto;

import com.ffxiv.planner.premium.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentHistoryResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant paidAt,
        String failureReason
) {}
