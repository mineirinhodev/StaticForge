package com.ffxiv.planner.premium.repository;

import com.ffxiv.planner.premium.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserId(UUID userId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

    @Query("SELECT s FROM Subscription s WHERE s.status IN ('ACTIVE', 'TRIALING') AND s.endDate < :today")
    List<Subscription> findExpired(@Param("today") LocalDate today);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'CANCELED' AND s.endDate < :today")
    List<Subscription> findCanceledAndExpired(@Param("today") LocalDate today);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE' AND s.endDate < :gracePeriodEnd")
    List<Subscription> findPastDueExpired(@Param("gracePeriodEnd") LocalDate gracePeriodEnd);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate <= :date")
    List<Subscription> findExpiringSoon(@Param("date") LocalDate date);
}
