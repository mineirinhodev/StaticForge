# PREMIUM-01: Migration V8 (subscriptions, payment_history)

## Escopo
- `V8__create_subscriptions_and_payment_history.sql`: tabelas para o módulo premium
- `V8MigrationTest`: valida schema com Testcontainers + PostgreSQL real

## Tabelas

### subscriptions
- 1:1 com `users` (FK + UNIQUE em `user_id`)
- Campos: `plan` (MONTHLY/ANNUAL), `status` (ACTIVE/CANCELED/PAST_DUE/EXPIRED/TRIALING), datas, flags Stripe, `cancel_at_period_end`, `trial_used`, `auto_renew`
- `stripe_subscription_id` tem UNIQUE constraint

### payment_history
- M:1 com `subscriptions`
- Campos: `amount` (NUMERIC 10,2), `currency` (3 chars), IDs Stripe, `status`, `paid_at`, `failure_reason`
- Sem `ON DELETE CASCADE` — histórico deve persistir

## Índices
- `idx_subscriptions_user_id`, `idx_subscriptions_stripe_customer_id` (partial), `idx_subscriptions_stripe_subscription_id` (partial)
- `idx_subscriptions_active` — partial para status ativos, usado pelo scheduler
- `idx_payment_history_subscription_id`, `idx_payment_history_stripe_payment_id` (partial)

## Links
- Issue pai: #24 (PREMIUM-01)
- Sub-issue TDD: #122 (fechada)
- Branch: `feature/premium-01-migration`
- PR: #191
