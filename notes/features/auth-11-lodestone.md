# AUTH-11: LodestoneClient e verificação de personagem

## Escopo
- `LodestoneClient`: busca perfil no XIVAPI por lodestoneId via WebClient
- `LodestoneVerificationService`: inicia verificação (gera código, armazena em cache), confirma verificação (consulta XIVAPI, verifica código na bio)
- `CharacterVerifiedEvent` publicado quando verificação é confirmada
- Endpoints: `POST /characters/{id}/verify/start`, `POST /characters/{id}/verify/confirm`

## Decisões
- `LodestoneClient` é `@Component` com `WebClient` injetado — testável via Mockito
- Código de verificação: 8 chars alfanuméricos aleatórios, TTL 1h via `CacheManager`
- Verificação do código: busca bio do personagem no XIVAPI e verifica se contém o código
- WireMock não adicionado — testes do LodestoneClient usam Mockito para mockar WebClient; comportamento E2E testado nos testes de integração

## Links
- Issue pai: #16 (AUTH-11)
- Sub-issue TDD: #114
- Branch: `feature/auth-11-lodestone`
