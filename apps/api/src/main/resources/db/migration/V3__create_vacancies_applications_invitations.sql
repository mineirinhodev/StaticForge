-- ============================================================
-- V3__create_vacancies_applications_invitations.sql
-- ============================================================
-- Creates the recruitment tables:
--   static_vacancies, applications, invitations
--
-- Reference: docs/features/02-static-management.md section 4.1
-- Issue: https://github.com/mineirinhodev/StaticForge/issues/34
-- ============================================================

-- Vagas abertas por uma static
CREATE TABLE static_vacancies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    static_id       UUID NOT NULL REFERENCES statics(id) ON DELETE CASCADE,
    role_needed     VARCHAR(10) NOT NULL,
    job_preferences JSONB,
    requirements    JSONB,
    description     VARCHAR(500),
    is_open         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Candidaturas de jogadores a vagas
CREATE TABLE applications (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vacancy_id       UUID NOT NULL REFERENCES static_vacancies(id) ON DELETE CASCADE,
    character_id     UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    message          VARCHAR(1000),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    applied_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at     TIMESTAMPTZ,
    response_message VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Convites enviados por statics a jogadores
CREATE TABLE invitations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    static_id    UUID NOT NULL REFERENCES statics(id) ON DELETE CASCADE,
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    message      VARCHAR(1000),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMPTZ,
    vacancy_id   UUID REFERENCES static_vacancies(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Indexes ──────────────────────────────────────────────────

-- Vagas abertas de uma static
CREATE INDEX idx_vacancies_static_id
    ON static_vacancies(static_id, is_open);

-- Candidaturas pendentes por vaga
CREATE INDEX idx_applications_vacancy_status
    ON applications(vacancy_id, status);

-- Minhas candidaturas por status
CREATE INDEX idx_applications_character_status
    ON applications(character_id, status);

-- Candidaturas antigas para job de expiração
CREATE INDEX idx_applications_applied_at
    ON applications(applied_at)
    WHERE status = 'PENDING';

-- Convites recebidos por um personagem
CREATE INDEX idx_invitations_character_status
    ON invitations(character_id, status);

-- Convites enviados por uma static
CREATE INDEX idx_invitations_static_status
    ON invitations(static_id, status);

-- Convites antigos para job de expiração
CREATE INDEX idx_invitations_sent_at
    ON invitations(sent_at)
    WHERE status = 'PENDING';
