# AUTH-02: JPA Entities

GitHub Issue: #7

## Objetivo
Criar as entidades JPA para o domínio de auth/profile:
`AuditableEntity`, `User`, `Character`, `CharacterJob`, `RefreshToken` e o enum `Role`.

## Referência
- `docs/features/01-auth-profile.md` seção 2.1

## Pacotes
- Entidades: `com.ffxiv.planner.auth.entity`
- Enum: `com.ffxiv.planner.auth.entity.Role`
- Auditing config: `com.ffxiv.planner.common.auditing`

## Decisões
- `AuditableEntity` usa Spring Data JPA Auditing (`@EnableJpaAuditing` na classe de config)
- `RefreshToken` NÃO estende `AuditableEntity` — tem `createdAt` próprio (não `updatedAt`)
- `Character` é nome reservado no Java? Não — mas colide com `java.lang.Character`. Usar
  import explícito ou nome de pacote qualificado nos testes.
- Profile `test` usa H2 — JSONB não é suportado; usar `columnDefinition = "text"` condicionalmente
  ou aceitar que o teste de entidade rode com Testcontainers PostgreSQL.
- Optamos por rodar os testes de entidade com Testcontainers (já configurado) para manter
  fidelidade ao banco real.

## Estratégia de teste (TDD)
- Testar que cada entidade pode ser persistida e recuperada (round-trip)
- Testar que `AuditableEntity` preenche `createdAt`/`updatedAt` automaticamente
- Testar relacionamentos (User → Characters, Character → Jobs, User → RefreshTokens)
- Testar o enum `Role` com `EnumType.STRING`
- Usar `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + Testcontainers
