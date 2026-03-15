# AUTH-03: Repositories

GitHub Issue: #8

## Objetivo
Criar os repositórios Spring Data JPA com queries customizadas.

## Referência
- `docs/features/01-auth-profile.md` seção 2.3

## Interfaces
- `UserRepository` — findByEmail, existsByEmail, findByOAuthProvider, findActiveById
- `CharacterRepository` — findByUserId, findByLodestoneId, existsByLodestoneId, findMainByUserId, findLFSByDataCenter
- `CharacterJobRepository` — findByCharacterId, findMainByCharacterId, existsByCharacterIdAndJobName
- `RefreshTokenRepository` — findByTokenHashAndRevokedAtIsNull, revokeAllByUserId, deleteExpired

## Pacote
`com.ffxiv.planner.auth.repository`

## Decisões
- `CharacterRepository` usa `GameCharacter` (renomeado em AUTH-02)
- `findLFSByDataCenter` retorna `Page<GameCharacter>` (paginado para matching)
- `revokeAllByUserId` e `deleteExpired` precisam de `@Modifying` + `@Transactional`
- Testes com `@DataJpaTest` + Testcontainers (mesmo padrão de AUTH-02, profile `datajpa`)

## Estratégia de teste (TDD)
- Testar cada query derivada e JPQL customizada
- Confirmar que `revokeAllByUserId` seta `revokedAt` sem deletar registros
- Confirmar que `deleteExpired` remove apenas tokens vencidos
- Confirmar paginação de `findLFSByDataCenter`
