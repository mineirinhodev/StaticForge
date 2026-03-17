package com.ffxiv.planner.premium.controller;

import com.ffxiv.planner.premium.dto.*;
import com.ffxiv.planner.premium.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints REST de assinaturas premium.
 *
 * <p>Todos os endpoints autenticados extraem o {@code userId} do JWT via
 * {@code @AuthenticationPrincipal}. O endpoint de planos é público.</p>
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/30
 */
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /** GET /subscriptions/plans — lista planos disponíveis (público). */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> getAvailablePlans() {
        return ResponseEntity.ok(subscriptionService.getAvailablePlans());
    }

    /** GET /subscriptions/me — assinatura atual do usuário autenticado. */
    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(userId));
    }

    /** POST /subscriptions/checkout — cria sessão de checkout no Stripe. */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutSessionResponse> createCheckout(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.createSubscription(userId, request));
    }

    /** POST /subscriptions/trial — ativa trial gratuito de 7 dias. */
    @PostMapping("/trial")
    public ResponseEntity<SubscriptionResponse> activateTrial(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.activateTrial(userId));
    }

    /** POST /subscriptions/reactivate — reativa assinatura cancelada. */
    @PostMapping("/reactivate")
    public ResponseEntity<SubscriptionResponse> reactivate(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(subscriptionService.reactivateSubscription(userId));
    }

    /** DELETE /subscriptions/me — cancela assinatura ao fim do período. */
    @DeleteMapping("/me")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userId));
    }

    /** GET /subscriptions/me/payments — histórico paginado de pagamentos. */
    @GetMapping("/me/payments")
    public ResponseEntity<Page<PaymentHistoryResponse>> getPaymentHistory(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getPaymentHistory(userId, pageable));
    }
}
