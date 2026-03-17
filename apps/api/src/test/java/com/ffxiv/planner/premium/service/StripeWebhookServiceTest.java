package com.ffxiv.planner.premium.service;

import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.auth.repository.UserRepository;
import com.ffxiv.planner.common.exception.ResourceNotFoundException;
import com.ffxiv.planner.premium.entity.*;
import com.ffxiv.planner.premium.event.PaymentFailedEvent;
import com.ffxiv.planner.premium.event.SubscriptionActivatedEvent;
import com.ffxiv.planner.premium.repository.PaymentHistoryRepository;
import com.ffxiv.planner.premium.repository.SubscriptionRepository;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para StripeWebhookService.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/28
 */
@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private StripeWebhookService webhookService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        webhookService = new StripeWebhookService(
                subscriptionRepository, paymentHistoryRepository,
                userRepository, subscriptionService, eventPublisher);
        userId = UUID.randomUUID();
        user = new User();
        user.setEmail("player@test.com");
    }

    // ── handleCheckoutSessionCompleted ────────────────────────

    @Test
    void checkoutCompleted_createsActiveSubscription() {
        Session session = buildCheckoutSession("sub_stripe_abc", "cus_abc");
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        webhookService.handleCheckoutSessionCompleted(session);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(saved.getStripeSubscriptionId()).isEqualTo("sub_stripe_abc");
        assertThat(saved.getStripeCustomerId()).isEqualTo("cus_abc");
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    void checkoutCompleted_setsUserPremium() {
        Session session = buildCheckoutSession("sub_stripe_abc", "cus_abc");
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        webhookService.handleCheckoutSessionCompleted(session);

        assertThat(user.isPremium()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void checkoutCompleted_registersPaymentHistory() {
        Session session = buildCheckoutSession("sub_stripe_abc", "cus_abc");
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        webhookService.handleCheckoutSessionCompleted(session);

        verify(paymentHistoryRepository).save(any(PaymentHistory.class));
    }

    @Test
    void checkoutCompleted_publishesActivatedEvent() {
        Session session = buildCheckoutSession("sub_stripe_abc", "cus_abc");
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        webhookService.handleCheckoutSessionCompleted(session);

        ArgumentCaptor<SubscriptionActivatedEvent> captor =
                ArgumentCaptor.forClass(SubscriptionActivatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void checkoutCompleted_throwsWhenUserNotFound() {
        Session session = buildCheckoutSession("sub_stripe_abc", "cus_abc");
        when(userRepository.findActiveById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.handleCheckoutSessionCompleted(session))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── handleInvoicePaid ─────────────────────────────────────

    @Test
    void invoicePaid_updatesEndDateAndRecordsPayment() {
        Subscription sub = buildActiveSubscription("sub_stripe_abc");
        sub.setUser(user);
        when(subscriptionRepository.findByStripeSubscriptionId("sub_stripe_abc"))
                .thenReturn(Optional.of(sub));
        when(paymentHistoryRepository.findByStripePaymentId("pi_paid_abc"))
                .thenReturn(Optional.empty());

        Invoice invoice = buildInvoice("sub_stripe_abc", "pi_paid_abc", "in_abc",
                499L, "usd", null);

        webhookService.handleInvoicePaid(invoice);

        assertThat(sub.getEndDate()).isNotNull();
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(paymentHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(captor.getValue().getStripePaymentId()).isEqualTo("pi_paid_abc");
    }

    @Test
    void invoicePaid_restoresActiveWhenPastDue() {
        Subscription sub = buildActiveSubscription("sub_stripe_abc");
        sub.setStatus(SubscriptionStatus.PAST_DUE);
        sub.setUser(user);
        when(subscriptionRepository.findByStripeSubscriptionId("sub_stripe_abc"))
                .thenReturn(Optional.of(sub));
        when(paymentHistoryRepository.findByStripePaymentId("pi_paid_abc"))
                .thenReturn(Optional.empty());

        webhookService.handleInvoicePaid(buildInvoice(
                "sub_stripe_abc", "pi_paid_abc", "in_abc", 499L, "usd", null));

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void invoicePaid_isIdempotentWhenPaymentAlreadyRecorded() {
        when(paymentHistoryRepository.findByStripePaymentId("pi_dup"))
                .thenReturn(Optional.of(new PaymentHistory()));

        webhookService.handleInvoicePaid(buildInvoice(
                "sub_stripe_abc", "pi_dup", "in_abc", 499L, "usd", null));

        verify(paymentHistoryRepository, never()).save(any());
    }

    // ── handleInvoicePaymentFailed ────────────────────────────

    @Test
    void invoicePaymentFailed_marksPastDue() {
        Subscription sub = buildActiveSubscription("sub_stripe_abc");
        sub.setUser(user);
        when(subscriptionRepository.findByStripeSubscriptionId("sub_stripe_abc"))
                .thenReturn(Optional.of(sub));

        Invoice invoice = buildInvoice("sub_stripe_abc", "pi_fail_abc", "in_fail",
                499L, "usd", "Your card was declined.");

        webhookService.handleInvoicePaymentFailed(invoice);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void invoicePaymentFailed_registersFailedPaymentHistory() {
        Subscription sub = buildActiveSubscription("sub_stripe_abc");
        sub.setUser(user);
        when(subscriptionRepository.findByStripeSubscriptionId("sub_stripe_abc"))
                .thenReturn(Optional.of(sub));

        webhookService.handleInvoicePaymentFailed(buildInvoice(
                "sub_stripe_abc", "pi_fail_abc", "in_fail", 499L, "usd", "Declined."));

        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(paymentHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(captor.getValue().getFailureReason()).isEqualTo("Declined.");
    }

    @Test
    void invoicePaymentFailed_publishesPaymentFailedEvent() {
        Subscription sub = buildActiveSubscription("sub_stripe_abc");
        sub.setUser(user);
        when(subscriptionRepository.findByStripeSubscriptionId("sub_stripe_abc"))
                .thenReturn(Optional.of(sub));

        webhookService.handleInvoicePaymentFailed(buildInvoice(
                "sub_stripe_abc", "pi_fail_abc", "in_fail", 499L, "usd", "Declined."));

        ArgumentCaptor<PaymentFailedEvent> captor =
                ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().failureReason()).isEqualTo("Declined.");
    }

    // ── handleSubscriptionDeleted ─────────────────────────────

    @Test
    void subscriptionDeleted_callsExpireSubscription() {
        UUID subId = UUID.randomUUID();
        Subscription sub = buildActiveSubscription("sub_stripe_abc");
        sub.setUser(user);
        when(subscriptionRepository.findByStripeSubscriptionId("sub_stripe_abc"))
                .thenReturn(Optional.of(sub));
        // mock getId via reflection not needed — we mock subscriptionService
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getId()).thenReturn("sub_stripe_abc");

        webhookService.handleSubscriptionDeleted(stripeSub);

        verify(subscriptionService).expireSubscription(any(UUID.class));
    }

    @Test
    void subscriptionDeleted_silentlyIgnoresWhenNotFound() {
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getId()).thenReturn("sub_unknown");
        when(subscriptionRepository.findByStripeSubscriptionId("sub_unknown"))
                .thenReturn(Optional.empty());

        assertThatNoException()
                .isThrownBy(() -> webhookService.handleSubscriptionDeleted(stripeSub));
        verify(subscriptionService, never()).expireSubscription(any());
    }

    // ── helpers ───────────────────────────────────────────────

    private Session buildCheckoutSession(String subscriptionId, String customerId) {
        Session session = mock(Session.class);
        lenient().when(session.getSubscription()).thenReturn(subscriptionId);
        lenient().when(session.getCustomer()).thenReturn(customerId);
        lenient().when(session.getAmountTotal()).thenReturn(499L);
        lenient().when(session.getCurrency()).thenReturn("usd");
        lenient().when(session.getPaymentIntent()).thenReturn("pi_session_abc");
        lenient().when(session.getMetadata()).thenReturn(
                java.util.Map.of("userId", userId.toString()));
        return session;
    }

    private Subscription buildActiveSubscription(String stripeSubId) {
        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID());
        sub.setPlan(SubscriptionPlan.MONTHLY);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(java.time.LocalDate.now().minusMonths(1));
        sub.setEndDate(java.time.LocalDate.now().plusDays(20));
        sub.setStripeSubscriptionId(stripeSubId);
        return sub;
    }

    private Invoice buildInvoice(String subscriptionId, String paymentIntentId,
                                  String invoiceId, Long amountPaid,
                                  String currency, String failureMessage) {
        Invoice invoice = mock(Invoice.class);
        lenient().when(invoice.getSubscription()).thenReturn(subscriptionId);
        lenient().when(invoice.getPaymentIntent()).thenReturn(paymentIntentId);
        lenient().when(invoice.getId()).thenReturn(invoiceId);
        lenient().when(invoice.getAmountPaid()).thenReturn(amountPaid);
        lenient().when(invoice.getCurrency()).thenReturn(currency);
        lenient().when(invoice.getPeriodEnd()).thenReturn(
                Instant.now().plusSeconds(30 * 24 * 3600).getEpochSecond());
        if (failureMessage != null) {
            com.stripe.model.StripeError error = mock(com.stripe.model.StripeError.class);
            when(error.getMessage()).thenReturn(failureMessage);
            lenient().when(invoice.getLastFinalizationError()).thenReturn(error);
        } else {
            lenient().when(invoice.getLastFinalizationError()).thenReturn(null);
        }
        return invoice;
    }
}
