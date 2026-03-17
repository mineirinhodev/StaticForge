# PREMIUM-07: SubscriptionController e DTOs

Issue: https://github.com/mineirinhodev/StaticForge/issues/30
TDD Issue: https://github.com/mineirinhodev/StaticForge/issues/128

## Escopo
Endpoints REST do `SubscriptionController`:
- `GET  /subscriptions/plans`       — lista planos disponíveis (público)
- `GET  /subscriptions/me`          — assinatura atual do usuário (autenticado)
- `POST /subscriptions/checkout`    — cria sessão de checkout Stripe (autenticado)
- `POST /subscriptions/trial`       — ativa trial gratuito (autenticado)
- `POST /subscriptions/reactivate`  — reativa assinatura cancelada (autenticado)
- `DELETE /subscriptions/me`        — cancela assinatura (autenticado)
- `GET  /subscriptions/me/payments` — histórico de pagamentos (autenticado, paginado)

## DTOs (já existem)
- `CreateSubscriptionRequest` — `plan` (@NotNull), `successUrl`, `cancelUrl`
- `SubscriptionResponse` — id, plan, status, startDate, endDate, cancelAtPeriodEnd, autoRenew, trialUsed, createdAt
- `CheckoutSessionResponse` — sessionId, checkoutUrl
- `PlanResponse` — planId, plan, name, price, currency, interval, savingsPercent, features
- `PaymentHistoryResponse` — id, amount, currency, status, paidAt, failureReason

## Pacote
`com.ffxiv.planner.premium.controller`

## Observações
- Padrão: `@WebMvcTest` + `@Import(SecurityConfig.class)` + `JwtFixtures` (como AuthControllerTest)
- `@AuthenticationPrincipal UUID userId` para extrair usuário do JWT
- Exceções mapeadas via `GlobalExceptionHandler` já existente
- Sem `SubscriptionMapper` (MapStruct): service já retorna DTOs diretamente
