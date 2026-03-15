package com.ffxiv.planner.premium.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abstrai todas as chamadas ao SDK do Stripe.
 *
 * <p>Nenhum objeto do Stripe deve vazar para camadas superiores —
 * o {@code SubscriptionService} usa apenas os tipos do domínio local.</p>
 */
@Service
public class StripeService {

    private final String webhookSecret;

    public StripeService(
            @Value("${app.stripe.secret-key}") String secretKey,
            @Value("${app.stripe.webhook-secret}") String webhookSecret) {
        Stripe.apiKey = secretKey;
        this.webhookSecret = webhookSecret;
    }

    /**
     * Busca um Customer existente pelo email ou cria um novo.
     *
     * @return stripeCustomerId
     */
    public String getOrCreateCustomer(String email, UUID userId) throws StripeException {
        CustomerListParams listParams = CustomerListParams.builder()
                .setEmail(email)
                .setLimit(1L)
                .build();

        CustomerCollection existing = Customer.list(listParams);
        List<Customer> customers = existing.getData();

        if (!customers.isEmpty()) {
            return customers.get(0).getId();
        }

        CustomerCreateParams createParams = CustomerCreateParams.builder()
                .setEmail(email)
                .putMetadata("userId", userId.toString())
                .build();

        return Customer.create(createParams).getId();
    }

    /**
     * Cria uma Checkout Session no Stripe para pagamento de assinatura.
     */
    public Session createCheckoutSession(
            String customerId,
            String priceId,
            String successUrl,
            String cancelUrl,
            Map<String, String> metadata) throws StripeException {

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build());

        metadata.forEach(builder::putMetadata);

        return Session.create(builder.build());
    }

    /**
     * Cancela uma assinatura no Stripe ao fim do período atual.
     */
    public void cancelSubscription(String stripeSubscriptionId) throws StripeException {
        com.stripe.model.Subscription subscription =
                com.stripe.model.Subscription.retrieve(stripeSubscriptionId);

        subscription.update(SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build());
    }

    /**
     * Reativa uma assinatura cancelada removendo o {@code cancel_at_period_end}.
     */
    public void reactivateSubscription(String stripeSubscriptionId) throws StripeException {
        com.stripe.model.Subscription subscription =
                com.stripe.model.Subscription.retrieve(stripeSubscriptionId);

        subscription.update(SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(false)
                .build());
    }

    /**
     * Constrói e verifica a assinatura HMAC de um evento de webhook do Stripe.
     *
     * @throws com.stripe.exception.SignatureVerificationException se inválida
     */
    public Event constructWebhookEvent(String payload, String sigHeader) throws StripeException {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }
}
