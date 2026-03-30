-- ============================================================
-- V2__create_statics_and_memberships.sql
-- ============================================================
-- Creates the core static group tables:
--   statics, static_memberships
--
-- Enables pg_trgm for GIN-based textual search on static names.
--
-- Reference: docs/features/02-static-management.md section 4.1
-- Issue: https://github.com/mineirinhodev/StaticForge/issues/34
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Tabela de statics (grupos fixos de raid)
CREATE TABLE statics (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(2000),
    content_scope JSONB,
    loot_rules    VARCHAR(500),
    language      VARCHAR(10),
    status        VARCHAR(20) NOT NULL DEFAULT 'RECRUITING',
    max_members   INTEGER NOT NULL DEFAULT 8,
    data_center   VARCHAR(20),
    server        VARCHAR(50),
    schedule      JSONB,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabela de membros da static
CREATE TABLE static_memberships (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    static_id    UUID NOT NULL REFERENCES statics(id) ON DELETE CASCADE,
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    role         VARCHAR(10),
    job_assigned VARCHAR(3),
    is_leader    BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_static_character UNIQUE (static_id, character_id)
);

-- ── Indexes ──────────────────────────────────────────────────

-- Busca de statics ativas filtradas por data center
CREATE INDEX idx_statics_status_dc
    ON statics(status, data_center)
    WHERE deleted_at IS NULL;

-- Busca textual por nome via trigram (GIN)
CREATE INDEX idx_statics_name_trgm
    ON statics USING gin (name gin_trgm_ops)
    WHERE deleted_at IS NULL;

-- Membros ativos de uma static
CREATE INDEX idx_memberships_static_id
    ON static_memberships(static_id)
    WHERE left_at IS NULL;

-- Statics ativas de um personagem
CREATE INDEX idx_memberships_character_id
    ON static_memberships(character_id)
    WHERE left_at IS NULL;
