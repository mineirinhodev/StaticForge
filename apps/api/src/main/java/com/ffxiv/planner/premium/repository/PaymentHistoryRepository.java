package com.ffxiv.planner.premium.repository;

import com.ffxiv.planner.premium.entity.PaymentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

    List<PaymentHistory> findBySubscriptionIdOrderByPaidAtDesc(UUID subscriptionId);

    Page<PaymentHistory> findBySubscriptionId(UUID subscriptionId, Pageable pageable);

    Optional<PaymentHistory> findByStripePaymentId(String stripePaymentId);
}
