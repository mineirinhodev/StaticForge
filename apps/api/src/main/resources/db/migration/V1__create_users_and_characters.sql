-- ============================================================
-- V1__create_users_and_characters.sql
-- ============================================================
-- Creates the foundational tables for authentication & profile:
--   users, characters, character_jobs, refresh_tokens
--
-- Reference: docs/features/01-auth-profile.md section 4.1
-- Issue: https://github.com/mineirinhodev/StaticForge/issues/6
-- ============================================================

-- Tabela de usuarios
CREATE TABLE users (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(255),
    locale                VARCHAR(10) DEFAULT 'en',
    is_premium            BOOLEAN NOT NULL DEFAULT FALSE,
    premium_expires_at    TIMESTAMPTZ,
    is_active             BOOLEAN NOT NULL DEFAULT FALSE,
    oauth_provider        VARCHAR(20),
    oauth_provider_id     VARCHAR(255),
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    deleted_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabela de personagens
CREATE TABLE characters (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name               VARCHAR(50) NOT NULL,
    server             VARCHAR(50) NOT NULL,
    data_center        VARCHAR(20) NOT NULL,
    lodestone_id       BIGINT UNIQUE,
    is_verified        BOOLEAN NOT NULL DEFAULT FALSE,
    is_main            BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_url         VARCHAR(500),
    race               VARCHAR(20),
    gender             VARCHAR(20),
    free_company       VARCHAR(100),
    bio                VARCHAR(1000),
    looking_for_static BOOLEAN NOT NULL DEFAULT FALSE,
    availability       JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabela de jobs do personagem
CREATE TABLE character_jobs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    job_name     VARCHAR(3) NOT NULL,
    role         VARCHAR(10) NOT NULL,
    is_main      BOOLEAN NOT NULL DEFAULT FALSE,
    ilvl         INTEGER,
    parse_tier   VARCHAR(20),
    progression  VARCHAR(100),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_character_job UNIQUE (character_id, job_name)
);

-- Tabela de refresh tokens
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMPTZ,
    replaced_by VARCHAR(64),
    user_agent  VARCHAR(500),
    ip_address  VARCHAR(45)
);

-- ── Standard Indexes ────────────────────────────────────────
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_characters_user_id ON characters(user_id);
CREATE INDEX idx_characters_data_center_server ON characters(data_center, server);
CREATE INDEX idx_character_jobs_character_id ON character_jobs(character_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- ── Partial Indexes (PostgreSQL-specific) ───────────────────
CREATE INDEX idx_users_oauth
    ON users(oauth_provider, oauth_provider_id)
    WHERE oauth_provider IS NOT NULL;

CREATE INDEX idx_characters_lodestone_id
    ON characters(lodestone_id)
    WHERE lodestone_id IS NOT NULL;

CREATE INDEX idx_characters_lfs
    ON characters(data_center, looking_for_static)
    WHERE looking_for_static = TRUE AND is_verified = TRUE;

CREATE INDEX idx_refresh_tokens_token_hash
    ON refresh_tokens(token_hash)
    WHERE revoked_at IS NULL;
