package com.ffxiv.planner.premium.service;

import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.common.exception.ResourceNotFoundException;
import com.ffxiv.planner.premium.dto.*;
import com.ffxiv.planner.premium.entity.*;
import com.ffxiv.planner.premium.event.*;
import com.ffxiv.planner.premium.exception.*;
import com.ffxiv.planner.premium.repository.PaymentHistoryRepository;
import com.ffxiv.planner.premium.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes de unidade para SubscriptionService.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/27
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StripeService stripeService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SubscriptionService subscriptionService;

    private static final String PRICE_MONTHLY  = "price_monthly_test";
    private static final String PRICE_ANNUAL   = "price_annual_test";
    private static final String FRONTEND_URL   = "http://localhost:3000";

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                subscriptionRepository,
                paymentHistoryRepository,
                userRepository,
                stripeService,
                eventPublisher,
                PRICE_MONTHLY,
                PRICE_ANNUAL,
                FRONTEND_URL);
        userId = UUID.randomUUID();
        user = new User();
        user.setEmail("player@test.com");
    }

    // ── getAvailablePlans ─────────────────────────────────────

    @Test
    void getAvailablePlans_returnsMonthlyAndAnnualPlans() {
        List<PlanResponse> plans = subscriptionService.getAvailablePlans();

        assertThat(plans).hasSize(2);
        assertThat(plans).extracting(PlanResponse::plan)
                .containsExactlyInAnyOrder(SubscriptionPlan.MONTHLY, SubscriptionPlan.ANNUAL);
    }

    @Test
    void getAvailablePlans_annualPlanHasSavingsPercent() {
        List<PlanResponse> plans = subscriptionService.getAvailablePlans();

        PlanResponse annual = plans.stream()
                .filter(p -> p.plan() == SubscriptionPlan.ANNUAL)
                .findFirst().orElseThrow();

        assertThat(annual.savingsPercent()).isPositive();
    }

    // ── createSubscription ────────────────────────────────────

    @Test
    void createSubscription_returnsCheckoutSessionResponse() throws StripeException {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(stripeService.getOrCreateCustomer(anyString(), eq(userId))).thenReturn("cus_123");

        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_abc");
        when(session.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_abc");
        when(stripeService.createCheckoutSession(
                eq("cus_123"), eq(PRICE_MONTHLY), anyString(), anyString(), anyMap()))
                .thenReturn(session);

        var request = new CreateSubscriptionRequest(
                SubscriptionPlan.MONTHLY,
                "https://app.com/success",
                "https://app.com/cancel");

        CheckoutSessionResponse response = subscriptionService.createSubscription(userId, request);

        assertThat(response.sessionId()).isEqualTo("cs_test_abc");
        assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_abc");
    }

    @Test
    void createSubscription_throwsWhenUserNotFound() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.empty());

        var request = new CreateSubscriptionRequest(SubscriptionPlan.MONTHLY, null, null);

        assertThatThrownBy(() -> subscriptionService.createSubscription(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createSubscription_throwsWhenAlreadyHasActiveSubscription() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));

        Subscription active = new Subscription();
        active.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(active));

        var request = new CreateSubscriptionRequest(SubscriptionPlan.MONTHLY, null, null);

        assertThatThrownBy(() -> subscriptionService.createSubscription(userId, request))
                .isInstanceOf(SubscriptionAlreadyActiveException.class);
    }

    // ── getMySubscription ─────────────────────────────────────

    @Test
    void getMySubscription_returnsSubscriptionResponse() {
        Subscription sub = buildActiveSubscription();
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        SubscriptionResponse response = subscriptionService.getMySubscription(userId);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.plan()).isEqualTo(SubscriptionPlan.MONTHLY);
    }

    @Test
    void getMySubscription_throwsWhenNoSubscription() {
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getMySubscription(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── cancelSubscription ────────────────────────────────────

    @Test
    void cancelSubscription_updatesStatusToCanceled() throws StripeException {
        Subscription sub = buildActiveSubscription();
        sub.setStripeSubscriptionId("sub_stripe123");
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        SubscriptionResponse response = subscriptionService.cancelSubscription(userId);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(response.cancelAtPeriodEnd()).isTrue();
        verify(stripeService).cancelSubscription("sub_stripe123");
    }

    @Test
    void cancelSubscription_publishesCanceledEvent() throws StripeException {
        Subscription sub = buildActiveSubscription();
        sub.setStripeSubscriptionId("sub_stripe123");
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        subscriptionService.cancelSubscription(userId);

        ArgumentCaptor<SubscriptionCanceledEvent> captor =
                ArgumentCaptor.forClass(SubscriptionCanceledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void cancelSubscription_throwsWhenStatusIsExpired() {
        Subscription sub = buildActiveSubscription();
        sub.setStatus(SubscriptionStatus.EXPIRED);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(userId))
                .isInstanceOf(CannotCancelSubscriptionException.class);
    }

    @Test
    void cancelSubscription_worksForTrialingStatus() throws StripeException {
        Subscription sub = buildActiveSubscription();
        sub.setStatus(SubscriptionStatus.TRIALING);
        sub.setStripeSubscriptionId("sub_trial123");
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        SubscriptionResponse response = subscriptionService.cancelSubscription(userId);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.CANCELED);
    }

    // ── reactivateSubscription ────────────────────────────────

    @Test
    void reactivateSubscription_updatesStatusToActive() throws StripeException {
        Subscription sub = buildActiveSubscription();
        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setStripeSubscriptionId("sub_stripe123");
        sub.setEndDate(LocalDate.now().plusDays(10));
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        SubscriptionResponse response = subscriptionService.reactivateSubscription(userId);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.cancelAtPeriodEnd()).isFalse();
        verify(stripeService).reactivateSubscription("sub_stripe123");
    }

    @Test
    void reactivateSubscription_publishesReactivatedEvent() throws StripeException {
        Subscription sub = buildActiveSubscription();
        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setStripeSubscriptionId("sub_stripe123");
        sub.setEndDate(LocalDate.now().plusDays(10));
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        subscriptionService.reactivateSubscription(userId);

        ArgumentCaptor<SubscriptionReactivatedEvent> captor =
                ArgumentCaptor.forClass(SubscriptionReactivatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void reactivateSubscription_throwsWhenStatusIsNotCanceled() {
        Subscription sub = buildActiveSubscription();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.reactivateSubscription(userId))
                .isInstanceOf(CannotReactivateSubscriptionException.class);
    }

    @Test
    void reactivateSubscription_throwsWhenEndDateHasPassed() {
        Subscription sub = buildActiveSubscription();
        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setEndDate(LocalDate.now().minusDays(1));
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.reactivateSubscription(userId))
                .isInstanceOf(CannotReactivateSubscriptionException.class);
    }

    // ── activateTrial ─────────────────────────────────────────

    @Test
    void activateTrial_createsTrialingSubscription() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = subscriptionService.activateTrial(userId);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(response.endDate()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(response.trialUsed()).isTrue();
    }

    @Test
    void activateTrial_throwsWhenTrialAlreadyUsed() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));

        Subscription expiredTrial = new Subscription();
        expiredTrial.setStatus(SubscriptionStatus.EXPIRED);
        expiredTrial.setTrialUsed(true);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(expiredTrial));

        assertThatThrownBy(() -> subscriptionService.activateTrial(userId))
                .isInstanceOf(TrialAlreadyUsedException.class);
    }

    @Test
    void activateTrial_throwsWhenAlreadyHasActiveSubscription() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));

        Subscription active = new Subscription();
        active.setStatus(SubscriptionStatus.ACTIVE);
        active.setTrialUsed(false);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> subscriptionService.activateTrial(userId))
                .isInstanceOf(SubscriptionAlreadyActiveException.class);
    }

    @Test
    void activateTrial_publishesActivatedEvent() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.activateTrial(userId);

        ArgumentCaptor<SubscriptionActivatedEvent> captor =
                ArgumentCaptor.forClass(SubscriptionActivatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
    }

    // ── expireSubscription ────────────────────────────────────

    @Test
    void expireSubscription_updatesStatusToExpired() {
        UUID subId = UUID.randomUUID();
        Subscription sub = buildActiveSubscription();
        sub.setUser(user);
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(sub));

        subscriptionService.expireSubscription(subId);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    void expireSubscription_publishesExpiredEvent() {
        UUID subId = UUID.randomUUID();
        Subscription sub = buildActiveSubscription();
        sub.setUser(user);
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(sub));

        subscriptionService.expireSubscription(subId);

        ArgumentCaptor<SubscriptionExpiredEvent> captor =
                ArgumentCaptor.forClass(SubscriptionExpiredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
    }

    @Test
    void expireSubscription_throwsWhenNotFound() {
        UUID subId = UUID.randomUUID();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.expireSubscription(subId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPaymentHistory ─────────────────────────────────────

    @Test
    void getPaymentHistory_returnsPageOfPayments() {
        Subscription sub = buildActiveSubscription();
        when(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub));

        PaymentHistory payment = new PaymentHistory();
        payment.setAmount(new BigDecimal("4.99"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(Instant.now());

        Page<PaymentHistory> page = new PageImpl<>(List.of(payment));
        when(paymentHistoryRepository.findBySubscriptionId(any(), any())).thenReturn(page);

        Page<PaymentHistoryResponse> result = subscriptionService.getPaymentHistory(
                userId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).amount()).isEqualByComparingTo("4.99");
    }

    // ── helpers ───────────────────────────────────────────────

    private Subscription buildActiveSubscription() {
        Subscription sub = new Subscription();
        sub.setPlan(SubscriptionPlan.MONTHLY);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(LocalDate.now().minusMonths(1));
        sub.setEndDate(LocalDate.now().plusDays(20));
        sub.setCancelAtPeriodEnd(false);
        sub.setTrialUsed(false);
        sub.setAutoRenew(true);
        sub.setUser(user);
        return sub;
    }
}
