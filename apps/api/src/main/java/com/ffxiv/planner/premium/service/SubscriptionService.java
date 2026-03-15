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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lógica de negócio de assinaturas premium.
 *
 * <p>Integra com {@link StripeService} para operações no Stripe e publica
 * eventos de domínio para notificações e atualização do status do usuário.</p>
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/27
 */
@Service
@Transactional
public class SubscriptionService {

    private static final int TRIAL_DAYS = 7;
    private static final BigDecimal MONTHLY_PRICE = new BigDecimal("4.99");
    private static final BigDecimal ANNUAL_PRICE = new BigDecimal("49.99");
    private static final BigDecimal ANNUAL_SAVINGS_PERCENT = new BigDecimal("16.51");

    private static final List<String> PREMIUM_FEATURES = List.of(
            "Unlimited statics",
            "Unlimited match suggestions",
            "Unlimited direct messages",
            "Create PUG events",
            "Export calendar (iCal)",
            "Advanced search filters",
            "Premium badge",
            "Priority support"
    );

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;
    private final ApplicationEventPublisher eventPublisher;
    private final String priceMonthly;
    private final String priceAnnual;
    private final String frontendUrl;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            PaymentHistoryRepository paymentHistoryRepository,
            UserRepository userRepository,
            StripeService stripeService,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.stripe.price-monthly}") String priceMonthly,
            @Value("${app.stripe.price-annual}") String priceAnnual,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
        this.eventPublisher = eventPublisher;
        this.priceMonthly = priceMonthly;
        this.priceAnnual = priceAnnual;
        this.frontendUrl = frontendUrl;
    }

    // ── getAvailablePlans ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PlanResponse> getAvailablePlans() {
        return List.of(
                new PlanResponse(
                        priceMonthly,
                        SubscriptionPlan.MONTHLY,
                        "Monthly Premium",
                        MONTHLY_PRICE,
                        "USD",
                        "month",
                        BigDecimal.ZERO,
                        PREMIUM_FEATURES),
                new PlanResponse(
                        priceAnnual,
                        SubscriptionPlan.ANNUAL,
                        "Annual Premium",
                        ANNUAL_PRICE,
                        "USD",
                        "year",
                        ANNUAL_SAVINGS_PERCENT,
                        List.of("All Monthly features", "Save 16.5% vs monthly"))
        );
    }

    // ── createSubscription ────────────────────────────────────

    public CheckoutSessionResponse createSubscription(UUID userId, CreateSubscriptionRequest request) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));

        subscriptionRepository.findByUserId(userId).ifPresent(sub -> {
            if (sub.getStatus().isActive()) {
                throw new SubscriptionAlreadyActiveException();
            }
        });

        try {
            String customerId = stripeService.getOrCreateCustomer(user.getEmail(), userId);
            String priceId = request.plan() == SubscriptionPlan.MONTHLY ? priceMonthly : priceAnnual;

            String successUrl = request.successUrl() != null
                    ? request.successUrl()
                    : frontendUrl + "/account/subscription?success=true";
            String cancelUrl = request.cancelUrl() != null
                    ? request.cancelUrl()
                    : frontendUrl + "/premium?canceled=true";

            var session = stripeService.createCheckoutSession(
                    customerId, priceId, successUrl, cancelUrl,
                    Map.of("userId", userId.toString()));

            return new CheckoutSessionResponse(session.getId(), session.getUrl());

        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Erro ao criar sessão de checkout no Stripe", e);
        }
    }

    // ── getMySubscription ─────────────────────────────────────

    @Transactional(readOnly = true)
    public SubscriptionResponse getMySubscription(UUID userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada para o usuário: " + userId));
        return toResponse(sub);
    }

    // ── cancelSubscription ────────────────────────────────────

    public SubscriptionResponse cancelSubscription(UUID userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada para o usuário: " + userId));

        if (sub.getStatus() != SubscriptionStatus.ACTIVE && sub.getStatus() != SubscriptionStatus.TRIALING) {
            throw new CannotCancelSubscriptionException(sub.getStatus());
        }

        try {
            if (sub.getStripeSubscriptionId() != null) {
                stripeService.cancelSubscription(sub.getStripeSubscriptionId());
            }
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Erro ao cancelar assinatura no Stripe", e);
        }

        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setCancelAtPeriodEnd(true);

        eventPublisher.publishEvent(new SubscriptionCanceledEvent(userId, sub.getEndDate()));

        return toResponse(sub);
    }

    // ── reactivateSubscription ────────────────────────────────

    public SubscriptionResponse reactivateSubscription(UUID userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada para o usuário: " + userId));

        if (sub.getStatus() != SubscriptionStatus.CANCELED) {
            throw CannotReactivateSubscriptionException.invalidStatus(sub.getStatus());
        }

        if (!sub.getEndDate().isAfter(LocalDate.now())) {
            throw CannotReactivateSubscriptionException.alreadyExpired(sub.getEndDate());
        }

        try {
            if (sub.getStripeSubscriptionId() != null) {
                stripeService.reactivateSubscription(sub.getStripeSubscriptionId());
            }
        } catch (com.stripe.exception.StripeException e) {
            throw new RuntimeException("Erro ao reativar assinatura no Stripe", e);
        }

        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCancelAtPeriodEnd(false);

        eventPublisher.publishEvent(new SubscriptionReactivatedEvent(userId));

        return toResponse(sub);
    }

    // ── activateTrial ─────────────────────────────────────────

    public SubscriptionResponse activateTrial(UUID userId) {
        // Busca o user uma única vez e reutiliza abaixo
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));

        subscriptionRepository.findByUserId(userId).ifPresent(existing -> {
            if (existing.isTrialUsed()) {
                throw new TrialAlreadyUsedException();
            }
            if (existing.getStatus().isActive()) {
                throw new SubscriptionAlreadyActiveException();
            }
        });

        Subscription trial = new Subscription();
        trial.setUser(user);
        trial.setPlan(SubscriptionPlan.MONTHLY);
        trial.setStatus(SubscriptionStatus.TRIALING);
        trial.setStartDate(LocalDate.now());
        trial.setEndDate(LocalDate.now().plusDays(TRIAL_DAYS));
        trial.setTrialUsed(true);
        trial.setCancelAtPeriodEnd(false);
        trial.setAutoRenew(false);

        Subscription saved = subscriptionRepository.save(trial);

        eventPublisher.publishEvent(new SubscriptionActivatedEvent(userId, saved.getPlan(), saved.getEndDate()));

        return toResponse(saved);
    }

    // ── getPaymentHistory ─────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponse> getPaymentHistory(UUID userId, Pageable pageable) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada para o usuário: " + userId));

        return paymentHistoryRepository
                .findBySubscriptionId(sub.getId(), pageable)
                .map(this::toPaymentResponse);
    }

    // ── expireSubscription ────────────────────────────────────

    public void expireSubscription(UUID subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada: " + subscriptionId));

        sub.setStatus(SubscriptionStatus.EXPIRED);

        UUID userId = sub.getUser().getId();
        eventPublisher.publishEvent(new SubscriptionExpiredEvent(userId));
    }

    // ── helpers ───────────────────────────────────────────────

    private SubscriptionResponse toResponse(Subscription sub) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getPlan(),
                sub.getStatus(),
                sub.getStartDate(),
                sub.getEndDate(),
                sub.isCancelAtPeriodEnd(),
                sub.isAutoRenew(),
                sub.isTrialUsed(),
                sub.getCreatedAt()
        );
    }

    private PaymentHistoryResponse toPaymentResponse(PaymentHistory payment) {
        return new PaymentHistoryResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getFailureReason()
        );
    }
}
