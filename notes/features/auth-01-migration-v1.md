# AUTH-01: Create migration V1

GitHub Issue: #6

## Objetivo
Criar a migration Flyway `V1__create_users_and_characters.sql` com as tabelas
`users`, `characters`, `character_jobs` e `refresh_tokens`.

## Referência
- `docs/features/01-auth-profile.md` seção 4.1 (DDL) e 4.2 (Índices)

## Decisões
- O DDL completo está definido no feature doc — seguir fielmente
- Flyway está desabilitado nos profiles `test` (H2) e `ci` (ddl-auto: update)
- Para validar a migration no PostgreSQL real, precisamos habilitar Flyway
  temporariamente ou rodar o SQL direto contra o container Docker
- O profile `dev` usa `ddl-auto: validate` + Flyway habilitado — é o cenário
  ideal para validar que a migration e as entidades JPA estão alinhadas

## Tabelas
1. `users` — conta do usuário (email, senha, OAuth, premium, lockout, soft delete)
2. `characters` — personagem FFXIV vinculado ao usuário
3. `character_jobs` — jobs do personagem (PLD, WHM, SAM, etc.) com role e stats
4. `refresh_tokens` — tokens de refresh JWT com suporte a rotação

## Índices (parciais/condicionais)
- `idx_users_oauth` — partial index WHERE oauth_provider IS NOT NULL
- `idx_characters_lodestone_id` — partial WHERE lodestone_id IS NOT NULL
- `idx_characters_lfs` — partial WHERE looking_for_static = TRUE AND is_verified = TRUE
- `idx_refresh_tokens_token_hash` — partial WHERE revoked_at IS NULL

## Estratégia de teste (TDD)
- Escrever teste que valida a migration roda sem erros contra PostgreSQL
- Verificar que todas as tabelas existem com colunas corretas
- Verificar que constraints e índices foram criados
- Usar o profile `test` com Flyway habilitado sobre H2 para testes rápidos,
  mas aceitar que partial indexes precisam de PostgreSQL real
