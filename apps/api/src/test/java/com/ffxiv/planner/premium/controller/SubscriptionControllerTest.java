package com.ffxiv.planner.premium.controller;

import com.ffxiv.planner.auth.jwt.JwtAuthenticationFilter;
import com.ffxiv.planner.auth.jwt.JwtProperties;
import com.ffxiv.planner.auth.jwt.JwtTokenProvider;
import com.ffxiv.planner.auth.security.JwtAuthenticationEntryPoint;
import com.ffxiv.planner.auth.security.SecurityConfig;
import com.ffxiv.planner.common.exception.ResourceNotFoundException;
import com.ffxiv.planner.premium.dto.*;
import com.ffxiv.planner.premium.entity.SubscriptionPlan;
import com.ffxiv.planner.premium.entity.SubscriptionStatus;
import com.ffxiv.planner.premium.exception.SubscriptionAlreadyActiveException;
import com.ffxiv.planner.premium.exception.TrialAlreadyUsedException;
import com.ffxiv.planner.premium.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes do SubscriptionController via MockMvc.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/30
 */
@WebMvcTest(SubscriptionController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        SubscriptionControllerTest.JwtFixtures.class})
class SubscriptionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @MockBean SubscriptionService subscriptionService;

    // ── GET /subscriptions/plans ──────────────────────────────

    @Test
    void getPlans_returns200WithPlanList() throws Exception {
        PlanResponse monthly = new PlanResponse(
                "price_monthly", SubscriptionPlan.MONTHLY, "Monthly Premium",
                new BigDecimal("4.99"), "USD", "month", BigDecimal.ZERO,
                List.of("Feature A"));
        when(subscriptionService.getAvailablePlans()).thenReturn(List.of(monthly));

        mockMvc.perform(get("/subscriptions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plan").value("MONTHLY"))
                .andExpect(jsonPath("$[0].price").value(4.99));
    }

    // ── GET /subscriptions/me ─────────────────────────────────

    @Test
    void getMySubscription_returns200ForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        SubscriptionResponse response = buildSubscriptionResponse(userId);
        when(subscriptionService.getMySubscription(userId)).thenReturn(response);

        mockMvc.perform(get("/subscriptions/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getMySubscription_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/subscriptions/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(subscriptionService);
    }

    @Test
    void getMySubscription_returns404WhenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        when(subscriptionService.getMySubscription(userId))
                .thenThrow(new ResourceNotFoundException("Assinatura não encontrada"));

        mockMvc.perform(get("/subscriptions/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── POST /subscriptions/checkout ──────────────────────────

    @Test
    void createCheckout_returns201WithSessionUrl() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        CheckoutSessionResponse session = new CheckoutSessionResponse(
                "cs_test_123", "https://checkout.stripe.com/pay/cs_test_123");
        when(subscriptionService.createSubscription(eq(userId), any()))
                .thenReturn(session);

        mockMvc.perform(post("/subscriptions/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plan":"MONTHLY"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("cs_test_123"))
                .andExpect(jsonPath("$.checkoutUrl").exists());
    }

    @Test
    void createCheckout_returns409WhenAlreadyActive() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        when(subscriptionService.createSubscription(eq(userId), any()))
                .thenThrow(new SubscriptionAlreadyActiveException());

        mockMvc.perform(post("/subscriptions/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plan":"ANNUAL"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createCheckout_returns422WhenPlanMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        mockMvc.perform(post("/subscriptions/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createCheckout_returns401WithoutToken() throws Exception {
        mockMvc.perform(post("/subscriptions/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plan":"MONTHLY"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /subscriptions/trial ─────────────────────────────

    @Test
    void activateTrial_returns201WithSubscription() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        SubscriptionResponse response = buildSubscriptionResponse(userId);
        when(subscriptionService.activateTrial(userId)).thenReturn(response);

        mockMvc.perform(post("/subscriptions/trial")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void activateTrial_returns409WhenTrialAlreadyUsed() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        when(subscriptionService.activateTrial(userId))
                .thenThrow(new TrialAlreadyUsedException());

        mockMvc.perform(post("/subscriptions/trial")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    // ── POST /subscriptions/reactivate ────────────────────────

    @Test
    void reactivate_returns200WithSubscription() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        SubscriptionResponse response = buildSubscriptionResponse(userId);
        when(subscriptionService.reactivateSubscription(userId)).thenReturn(response);

        mockMvc.perform(post("/subscriptions/reactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ── DELETE /subscriptions/me ──────────────────────────────

    @Test
    void cancelSubscription_returns200WithCanceledSubscription() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        SubscriptionResponse canceled = new SubscriptionResponse(
                UUID.randomUUID(), SubscriptionPlan.MONTHLY, SubscriptionStatus.CANCELED,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(20),
                true, false, false, Instant.now());
        when(subscriptionService.cancelSubscription(userId)).thenReturn(canceled);

        mockMvc.perform(delete("/subscriptions/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancelSubscription_returns401WithoutToken() throws Exception {
        mockMvc.perform(delete("/subscriptions/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(subscriptionService);
    }

    // ── GET /subscriptions/me/payments ────────────────────────

    @Test
    void getPaymentHistory_returns200WithPage() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        PaymentHistoryResponse payment = new PaymentHistoryResponse(
                UUID.randomUUID(), new BigDecimal("4.99"), "USD",
                com.ffxiv.planner.premium.entity.PaymentStatus.SUCCEEDED,
                Instant.now(), null);
        when(subscriptionService.getPaymentHistory(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));

        mockMvc.perform(get("/subscriptions/me/payments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currency").value("USD"));
    }

    // ── helpers ───────────────────────────────────────────────

    private SubscriptionResponse buildSubscriptionResponse(UUID userId) {
        return new SubscriptionResponse(
                UUID.randomUUID(), SubscriptionPlan.MONTHLY, SubscriptionStatus.ACTIVE,
                LocalDate.now(), LocalDate.now().plusDays(30),
                false, true, false, Instant.now());
    }

    // ── fixtures ──────────────────────────────────────────────

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
