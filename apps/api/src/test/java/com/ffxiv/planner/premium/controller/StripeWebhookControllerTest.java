package com.ffxiv.planner.premium.controller;

import com.ffxiv.planner.auth.jwt.JwtAuthenticationFilter;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import com.ffxiv.planner.auth.security.JwtAuthenticationEntryPoint;
import com.ffxiv.planner.auth.security.SecurityConfig;
import com.ffxiv.planner.premium.service.StripeService;
import com.ffxiv.planner.premium.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração HTTP para StripeWebhookController.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/28
 */
@WebMvcTest(StripeWebhookController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        StripeWebhookControllerTest.JwtFixtures.class})
class StripeWebhookControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean StripeService stripeService;
    @MockBean StripeWebhookService webhookService;

    private static final String VALID_SIG  = "t=123,v1=abc";
    private static final String PAYLOAD    = "{\"type\":\"checkout.session.completed\"}";

    // ── POST /webhooks/stripe — segurança ─────────────────────

    @Test
    void webhook_isPublicEndpointNoAuthRequired() throws Exception {
        Event event = buildEvent("checkout.session.completed");
        when(stripeService.constructWebhookEvent(any(), any())).thenReturn(event);

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", VALID_SIG)
                        .content(PAYLOAD))
                .andExpect(status().isOk());
    }

    // ── checkout.session.completed ────────────────────────────

    @Test
    void webhook_checkoutCompleted_returns200AndDelegates() throws Exception {
        Event event = buildEvent("checkout.session.completed");
        Session session = mock(Session.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(session));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(stripeService.constructWebhookEvent(any(), any())).thenReturn(event);

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", VALID_SIG)
                        .content(PAYLOAD))
                .andExpect(status().isOk());

        verify(webhookService).handleCheckoutSessionCompleted(session);
    }

    // ── invoice.paid ──────────────────────────────────────────

    @Test
    void webhook_invoicePaid_returns200AndDelegates() throws Exception {
        Event event = buildEvent("invoice.paid");
        Invoice invoice = mock(Invoice.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(invoice));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(stripeService.constructWebhookEvent(any(), any())).thenReturn(event);

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", VALID_SIG)
                        .content("{\"type\":\"invoice.paid\"}"))
                .andExpect(status().isOk());

        verify(webhookService).handleInvoicePaid(invoice);
    }

    // ── invoice.payment_failed ────────────────────────────────

    @Test
    void webhook_invoicePaymentFailed_returns200AndDelegates() throws Exception {
        Event event = buildEvent("invoice.payment_failed");
        Invoice invoice = mock(Invoice.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(invoice));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(stripeService.constructWebhookEvent(any(), any())).thenReturn(event);

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", VALID_SIG)
                        .content("{\"type\":\"invoice.payment_failed\"}"))
                .andExpect(status().isOk());

        verify(webhookService).handleInvoicePaymentFailed(invoice);
    }

    // ── customer.subscription.deleted ────────────────────────

    @Test
    void webhook_subscriptionDeleted_returns200AndDelegates() throws Exception {
        Event event = buildEvent("customer.subscription.deleted");
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSub));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(stripeService.constructWebhookEvent(any(), any())).thenReturn(event);

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", VALID_SIG)
                        .content("{\"type\":\"customer.subscription.deleted\"}"))
                .andExpect(status().isOk());

        verify(webhookService).handleSubscriptionDeleted(stripeSub);
    }

    // ── assinatura inválida ───────────────────────────────────

    @Test
    void webhook_returns400WhenSignatureInvalid() throws Exception {
        when(stripeService.constructWebhookEvent(any(), any()))
                .thenThrow(new SignatureVerificationException("Invalid", "bad_sig"));

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", "bad_sig")
                        .content(PAYLOAD))
                .andExpect(status().isBadRequest());
    }

    // ── evento desconhecido ───────────────────────────────────

    @Test
    void webhook_returns200ForUnknownEventType() throws Exception {
        Event event = buildEvent("some.unknown.event");
        when(stripeService.constructWebhookEvent(any(), any())).thenReturn(event);

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Stripe-Signature", VALID_SIG)
                        .content("{\"type\":\"some.unknown.event\"}"))
                .andExpect(status().isOk());

        verifyNoInteractions(webhookService);
    }

    // ── helpers ───────────────────────────────────────────────

    private Event buildEvent(String type) {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(type);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.empty());
        lenient().when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }

    @TestConfiguration
    static class JwtFixtures {
        @Bean
        JwtTokenProvider jwtTokenProvider() {
            JwtProperties props = new JwtProperties();
            props.setSecret("test-jwt-secret-key-minimum-32-characters-long-for-hs256");
            props.setAccessExpiration(java.time.Duration.ofMinutes(15));
            props.setRefreshExpiration(java.time.Duration.ofDays(30));
            return new JwtTokenProvider(props);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider) {
            return new JwtAuthenticationFilter(provider);
        }
    }
}
