-- ============================================================
-- V8__create_subscriptions_and_payment_history.sql
-- ============================================================
-- Creates tables for the Premium subscription module:
--   subscriptions, payment_history
--
-- Reference: docs/features/07-premium.md section 2.1
-- Issue: https://github.com/mineirinhodev/StaticForge/issues/24
-- ============================================================

-- Tabela de assinaturas premium
CREATE TABLE subscriptions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    plan                    VARCHAR(20) NOT NULL,   -- MONTHLY, ANNUAL
    status                  VARCHAR(20) NOT NULL,   -- ACTIVE, CANCELED, PAST_DUE, EXPIRED, TRIALING
    start_date              DATE NOT NULL,
    end_date                DATE NOT NULL,
    cancel_at_period_end    BOOLEAN NOT NULL DEFAULT FALSE,
    trial_used              BOOLEAN NOT NULL DEFAULT FALSE,
    stripe_customer_id      VARCHAR(100),
    stripe_subscription_id  VARCHAR(100) UNIQUE,
    auto_renew              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabela de histórico de pagamentos
CREATE TABLE payment_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id     UUID NOT NULL REFERENCES subscriptions(id),
    amount              NUMERIC(10, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL,          -- USD, EUR, BRL, JPY
    stripe_payment_id   VARCHAR(100) UNIQUE,
    stripe_invoice_id   VARCHAR(100),
    status              VARCHAR(20) NOT NULL,          -- SUCCEEDED, FAILED, REFUNDED, PENDING
    paid_at             TIMESTAMPTZ,
    failure_reason      VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices em subscriptions
CREATE INDEX idx_subscriptions_user_id
    ON subscriptions(user_id);

CREATE INDEX idx_subscriptions_stripe_customer_id
    ON subscriptions(stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;

CREATE INDEX idx_subscriptions_stripe_subscription_id
    ON subscriptions(stripe_subscription_id)
    WHERE stripe_subscription_id IS NOT NULL;

CREATE INDEX idx_subscriptions_active
    ON subscriptions(status, end_date)
    WHERE status IN ('ACTIVE', 'TRIALING', 'CANCELED', 'PAST_DUE');

-- Índices em payment_history
CREATE INDEX idx_payment_history_subscription_id
    ON payment_history(subscription_id);

CREATE INDEX idx_payment_history_stripe_payment_id
    ON payment_history(stripe_payment_id)
    WHERE stripe_payment_id IS NOT NULL;
