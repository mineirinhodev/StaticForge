# PREMIUM-05: StripeWebhookController

Issue TDD: #126 | Issue original: #28

## Escopo
POST `/api/v1/webhooks/stripe` — recebe e valida webhooks do Stripe via assinatura HMAC.

## Eventos tratados
| Evento | Ação |
|--------|------|
| `checkout.session.completed` | Cria Subscription no DB, ativa premium no User, registra PaymentHistory |
| `invoice.paid` | Atualiza endDate, registra PaymentHistory SUCCEEDED, restaura ACTIVE se PAST_DUE |
| `invoice.payment_failed` | Marca PAST_DUE, registra PaymentHistory FAILED com failureReason |
| `customer.subscription.deleted` | Expira assinatura (chama expireSubscription) |

## Decisões de design
- Endpoint público (sem JWT), verificado por assinatura HMAC via `StripeService.constructWebhookEvent`
- Idempotência via `stripe_payment_id` UNIQUE — duplicatas silenciosamente ignoradas
- Responsabilidade de criar Subscription é do webhook (não do createSubscription do service)
- StripeWebhookService separa lógica dos handlers do controller HTTP
- SecurityConfig precisa de `.permitAll()` em POST `/webhooks/stripe`

## Dependências
- `StripeService` (PREMIUM-03) — já disponível
- `SubscriptionService.expireSubscription` (PREMIUM-04) — cherry-picked
- `SubscriptionRepository`, `PaymentHistoryRepository` (PREMIUM-04)
- `UserRepository` (auth)
