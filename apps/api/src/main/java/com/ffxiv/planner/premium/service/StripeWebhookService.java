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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Lógica de negócio para processamento de webhooks do Stripe.
 *
 * <p>Separado do {@link StripeWebhookController} para facilitar testes
 * unitários sem a camada HTTP.</p>
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/28
 */
@Service
@Transactional
public class StripeWebhookService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final ApplicationEventPublisher eventPublisher;

    public StripeWebhookService(
            SubscriptionRepository subscriptionRepository,
            PaymentHistoryRepository paymentHistoryRepository,
            UserRepository userRepository,
            SubscriptionService subscriptionService,
            ApplicationEventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
        this.eventPublisher = eventPublisher;
    }

    // ── checkout.session.completed ────────────────────────────

    /**
     * Cria Subscription no DB, ativa premium no User, registra PaymentHistory.
     */
    public void handleCheckoutSessionCompleted(Session session) {
        UUID userId = UUID.fromString(session.getMetadata().get("userId"));

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));

        // Determina o plano pelo valor da sessão (mensal = padrão)
        SubscriptionPlan plan = SubscriptionPlan.MONTHLY;

        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusMonths(plan == SubscriptionPlan.MONTHLY ? 1 : 12);

        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setPlan(plan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(now);
        sub.setEndDate(endDate);
        sub.setStripeSubscriptionId(session.getSubscription());
        sub.setStripeCustomerId(session.getCustomer());
        sub.setCancelAtPeriodEnd(false);
        sub.setAutoRenew(true);
        sub.setTrialUsed(false);

        Subscription saved = subscriptionRepository.save(sub);

        // Ativa premium no user
        user.setPremium(true);
        user.setPremiumExpiresAt(endDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        userRepository.save(user);

        // Registra pagamento
        PaymentHistory payment = buildPaymentHistory(
                saved,
                session.getPaymentIntent(),
                null,
                session.getAmountTotal() != null
                        ? BigDecimal.valueOf(session.getAmountTotal(), 2)
                        : BigDecimal.ZERO,
                session.getCurrency() != null ? session.getCurrency().toUpperCase() : "USD",
                PaymentStatus.SUCCEEDED,
                null);
        paymentHistoryRepository.save(payment);

        eventPublisher.publishEvent(new SubscriptionActivatedEvent(userId, plan, endDate));
    }

    // ── invoice.paid ──────────────────────────────────────────

    /**
     * Renova o período da assinatura e registra pagamento. Idempotente via stripe_payment_id.
     */
    public void handleInvoicePaid(Invoice invoice) {
        // Idempotência: ignora se já processado
        if (paymentHistoryRepository.findByStripePaymentId(invoice.getPaymentIntent()).isPresent()) {
            return;
        }

        Subscription sub = subscriptionRepository
                .findByStripeSubscriptionId(invoice.getSubscription())
                .orElse(null);

        if (sub == null) return;

        // Atualiza endDate com o período do invoice
        if (invoice.getPeriodEnd() != null) {
            sub.setEndDate(LocalDate.ofInstant(
                    Instant.ofEpochSecond(invoice.getPeriodEnd()), ZoneOffset.UTC));
        }

        // Restaura ACTIVE se estava PAST_DUE
        if (sub.getStatus() == SubscriptionStatus.PAST_DUE) {
            sub.setStatus(SubscriptionStatus.ACTIVE);
        }

        PaymentHistory payment = buildPaymentHistory(
                sub,
                invoice.getPaymentIntent(),
                invoice.getId(),
                BigDecimal.valueOf(invoice.getAmountPaid(), 2),
                invoice.getCurrency().toUpperCase(),
                PaymentStatus.SUCCEEDED,
                null);
        paymentHistoryRepository.save(payment);
    }

    // ── invoice.payment_failed ────────────────────────────────

    /**
     * Marca assinatura como PAST_DUE e notifica o usuário.
     */
    public void handleInvoicePaymentFailed(Invoice invoice) {
        Subscription sub = subscriptionRepository
                .findByStripeSubscriptionId(invoice.getSubscription())
                .orElse(null);

        if (sub == null) return;

        sub.setStatus(SubscriptionStatus.PAST_DUE);

        String failureReason = invoice.getLastFinalizationError() != null
                ? invoice.getLastFinalizationError().getMessage()
                : extractFailureReason(invoice);

        PaymentHistory payment = buildPaymentHistory(
                sub,
                invoice.getPaymentIntent(),
                invoice.getId(),
                BigDecimal.valueOf(invoice.getAmountPaid() != null ? invoice.getAmountPaid() : 0L, 2),
                invoice.getCurrency() != null ? invoice.getCurrency().toUpperCase() : "USD",
                PaymentStatus.FAILED,
                failureReason);
        paymentHistoryRepository.save(payment);

        UUID userId = sub.getUser().getId();
        eventPublisher.publishEvent(new PaymentFailedEvent(userId, failureReason));
    }

    // ── customer.subscription.deleted ────────────────────────

    /**
     * Expira a assinatura local quando o Stripe a deleta.
     * Ignora silenciosamente se não encontrada (pode ter sido expirada pelo scheduler).
     */
    public void handleSubscriptionDeleted(com.stripe.model.Subscription stripeSub) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId())
                .ifPresent(sub -> subscriptionService.expireSubscription(sub.getId()));
    }

    // ── helpers ───────────────────────────────────────────────

    private PaymentHistory buildPaymentHistory(Subscription sub, String paymentIntentId,
                                               String invoiceId, BigDecimal amount,
                                               String currency, PaymentStatus status,
                                               String failureReason) {
        PaymentHistory payment = new PaymentHistory();
        payment.setSubscription(sub);
        payment.setStripePaymentId(paymentIntentId);
        payment.setStripeInvoiceId(invoiceId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(status);
        payment.setPaidAt(status == PaymentStatus.SUCCEEDED ? Instant.now() : null);
        payment.setFailureReason(failureReason);
        payment.setCreatedAt(Instant.now());
        return payment;
    }

    private String extractFailureReason(Invoice invoice) {
        // Tenta extrair reason de campos disponíveis na invoice
        try {
            if (invoice.getLastFinalizationError() != null) {
                return invoice.getLastFinalizationError().getMessage();
            }
        } catch (Exception ignored) {}
        return "Payment failed";
    }
}
