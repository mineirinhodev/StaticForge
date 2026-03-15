package com.ffxiv.planner.premium.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.model.checkout.Session;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testa StripeService isolando o SDK do Stripe com mockStatic.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/26
 */
@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    private static final String SECRET_KEY = "sk_test_key";
    private static final String WEBHOOK_SECRET = "whsec_test";

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService(SECRET_KEY, WEBHOOK_SECRET);
    }

    // ── getOrCreateCustomer ───────────────────────────────────

    @Test
    void getOrCreateCustomer_returnsExistingCustomerIdWhenFound() throws Exception {
        String email = "user@test.com";
        UUID userId = UUID.randomUUID();

        Customer existing = mock(Customer.class);
        when(existing.getId()).thenReturn("cus_existing123");

        CustomerCollection collection = mock(CustomerCollection.class);
        when(collection.getData()).thenReturn(List.of(existing));

        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {
            customerMock.when(() -> Customer.list(any(CustomerListParams.class)))
                    .thenReturn(collection);

            String result = stripeService.getOrCreateCustomer(email, userId);

            assertThat(result).isEqualTo("cus_existing123");
            customerMock.verify(() -> Customer.create(any(CustomerCreateParams.class)), never());
        }
    }

    @Test
    void getOrCreateCustomer_createsNewCustomerWhenNotFound() throws Exception {
        String email = "new@test.com";
        UUID userId = UUID.randomUUID();

        CustomerCollection emptyCollection = mock(CustomerCollection.class);
        when(emptyCollection.getData()).thenReturn(List.of());

        Customer newCustomer = mock(Customer.class);
        when(newCustomer.getId()).thenReturn("cus_new456");

        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {
            customerMock.when(() -> Customer.list(any(CustomerListParams.class)))
                    .thenReturn(emptyCollection);
            customerMock.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(newCustomer);

            String result = stripeService.getOrCreateCustomer(email, userId);

            assertThat(result).isEqualTo("cus_new456");
        }
    }

    // ── createCheckoutSession ─────────────────────────────────

    @Test
    void createCheckoutSession_returnsSessionWithUrlAndId() throws Exception {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test123");
        when(session.getUrl()).thenReturn("https://checkout.stripe.com/test");

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(session);

            Session result = stripeService.createCheckoutSession(
                    "cus_123", "price_monthly",
                    "https://app.com/success", "https://app.com/cancel",
                    Map.of("userId", UUID.randomUUID().toString()));

            assertThat(result.getId()).isEqualTo("cs_test123");
            assertThat(result.getUrl()).isEqualTo("https://checkout.stripe.com/test");
        }
    }

    // ── cancelSubscription ────────────────────────────────────

    @Test
    void cancelSubscription_setsCancelAtPeriodEnd() throws Exception {
        Subscription stripeSub = mock(Subscription.class);
        when(stripeSub.update(any(SubscriptionUpdateParams.class))).thenReturn(stripeSub);

        try (MockedStatic<Subscription> subMock = mockStatic(Subscription.class)) {
            subMock.when(() -> Subscription.retrieve(eq("sub_123")))
                    .thenReturn(stripeSub);

            stripeService.cancelSubscription("sub_123");

            verify(stripeSub).update(argThat((SubscriptionUpdateParams params) ->
                    Boolean.TRUE.equals(params.getCancelAtPeriodEnd())));
        }
    }

    // ── reactivateSubscription ────────────────────────────────

    @Test
    void reactivateSubscription_removesCancelAtPeriodEnd() throws Exception {
        Subscription stripeSub = mock(Subscription.class);
        when(stripeSub.update(any(SubscriptionUpdateParams.class))).thenReturn(stripeSub);

        try (MockedStatic<Subscription> subMock = mockStatic(Subscription.class)) {
            subMock.when(() -> Subscription.retrieve(eq("sub_456")))
                    .thenReturn(stripeSub);

            stripeService.reactivateSubscription("sub_456");

            verify(stripeSub).update(argThat((SubscriptionUpdateParams params) ->
                    Boolean.FALSE.equals(params.getCancelAtPeriodEnd())));
        }
    }

    // ── constructWebhookEvent ─────────────────────────────────

    @Test
    void constructWebhookEvent_returnsEventOnValidSignature() throws Exception {
        com.stripe.model.Event event = mock(com.stripe.model.Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(
                    eq("{\"type\":\"checkout.session.completed\"}"),
                    eq("t=123,v1=abc"),
                    eq(WEBHOOK_SECRET))).thenReturn(event);

            com.stripe.model.Event result = stripeService.constructWebhookEvent(
                    "{\"type\":\"checkout.session.completed\"}", "t=123,v1=abc");

            assertThat(result.getType()).isEqualTo("checkout.session.completed");
        }
    }

    @Test
    void constructWebhookEvent_throwsOnInvalidSignature() {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException(
                            "Invalid signature", "sig_header"));

            assertThatThrownBy(() -> stripeService.constructWebhookEvent("payload", "bad_sig"))
                    .isInstanceOf(com.stripe.exception.SignatureVerificationException.class);
        }
    }
}
