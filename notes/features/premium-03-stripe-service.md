# PREMIUM-03: StripeService

Issue TDD: #124 | Issue original: #26

## Escopo
Integração com Stripe SDK: `getOrCreateCustomer`, `createCheckoutSession`,
`cancelSubscription`, `reactivateSubscription`, `constructWebhookEvent`.

## Decisões

- `StripeService` abstrai 100% do SDK — nenhum tipo do Stripe vaza para
  camadas superiores (SubscriptionService usa apenas tipos do domínio).
- `Stripe.apiKey` é setado no construtor via `@Value("${app.stripe.secret-key}")`.
- `cancelSubscription` usa `cancelAtPeriodEnd=true` (não cancela imediatamente).
- `reactivateSubscription` remove o `cancelAtPeriodEnd` setando para `false`.

## Observação sobre stripe-java 28.x

`SignatureVerificationException` na versão 28+ tem apenas 2 parâmetros no
construtor: `(String message, String sigHeader)`. O terceiro parâmetro
`Throwable cause` foi removido — testes que usavam 3 argumentos precisam
ser corrigidos.

## Configuração

`application.yml` sob `app.stripe`:
- `secret-key`: env `STRIPE_SECRET_KEY`
- `webhook-secret`: env `STRIPE_WEBHOOK_SECRET`
- `price-monthly`: env `STRIPE_PRICE_MONTHLY`
- `price-annual`: env `STRIPE_PRICE_ANNUAL`

## Testes (7 ao todo)

| Teste | Comportamento |
|-------|--------------|
| `getOrCreateCustomer_returnsExistingCustomerIdWhenFound` | Retorna ID existente sem criar novo |
| `getOrCreateCustomer_createsNewCustomerWhenNotFound` | Cria customer quando não existe |
| `createCheckoutSession_returnsSessionWithUrlAndId` | Retorna session com id e url |
| `cancelSubscription_setsCancelAtPeriodEnd` | Chama update com `cancelAtPeriodEnd=true` |
| `reactivateSubscription_removesCancelAtPeriodEnd` | Chama update com `cancelAtPeriodEnd=false` |
| `constructWebhookEvent_returnsEventOnValidSignature` | Retorna evento com assinatura válida |
| `constructWebhookEvent_throwsOnInvalidSignature` | Lança `SignatureVerificationException` |
