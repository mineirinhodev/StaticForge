package com.ffxiv.planner.premium.controller;

import com.ffxiv.planner.premium.service.StripeService;
import com.ffxiv.planner.premium.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recebe webhooks do Stripe via POST público, verificado por assinatura HMAC.
 *
 * <p>O endpoint é público (sem JWT) mas a autenticidade é garantida pelo
 * {@link StripeService#constructWebhookEvent(String, String)} que valida
 * a assinatura HMAC do header {@code Stripe-Signature}.</p>
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/28
 */
@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeService stripeService;
    private final StripeWebhookService webhookService;

    public StripeWebhookController(StripeService stripeService,
                                   StripeWebhookService webhookService) {
        this.stripeService = stripeService;
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = stripeService.constructWebhookEvent(payload, sigHeader);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook com assinatura inválida: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Erro ao processar webhook do Stripe", e);
            return ResponseEntity.badRequest().body("Webhook error");
        }

        String type = event.getType();
        log.debug("Webhook recebido: {}", type);

        switch (type) {
            case "checkout.session.completed" -> {
                event.getDataObjectDeserializer().getObject()
                        .filter(Session.class::isInstance)
                        .map(Session.class::cast)
                        .ifPresent(webhookService::handleCheckoutSessionCompleted);
            }
            case "invoice.paid" -> {
                event.getDataObjectDeserializer().getObject()
                        .filter(Invoice.class::isInstance)
                        .map(Invoice.class::cast)
                        .ifPresent(webhookService::handleInvoicePaid);
            }
            case "invoice.payment_failed" -> {
                event.getDataObjectDeserializer().getObject()
                        .filter(Invoice.class::isInstance)
                        .map(Invoice.class::cast)
                        .ifPresent(webhookService::handleInvoicePaymentFailed);
            }
            case "customer.subscription.deleted" -> {
                event.getDataObjectDeserializer().getObject()
                        .filter(com.stripe.model.Subscription.class::isInstance)
                        .map(com.stripe.model.Subscription.class::cast)
                        .ifPresent(webhookService::handleSubscriptionDeleted);
            }
            default -> log.debug("Evento Stripe ignorado: {}", type);
        }

        return ResponseEntity.ok("received");
    }
}
