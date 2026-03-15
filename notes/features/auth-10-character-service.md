# AUTH-10: CharacterService e CharacterController

## Escopo
- `CharacterService`: `createCharacter`, `listCharacters`, `setMainCharacter`, `deleteCharacter`
- `CharacterController`: `GET /characters`, `POST /characters`, `PUT /characters/{id}/main`, `DELETE /characters/{id}`
- Regras de negócio:
  - Primeiro personagem criado vira main automaticamente
  - `isMain=true` no request desmarca o main atual antes de definir o novo
  - Não é possível remover o único personagem
  - Se main removido, outro personagem é promovido a main
  - Ownership: usuário só opera seus próprios personagens (`ForbiddenException`)

## Decisões
- Pacote `com.ffxiv.planner.profile` (junto com ProfileService)
- `CharacterCreateRequest` e `ForbiddenException` criados nesta branch
- `CharacterCreatedEvent` publicado via `ApplicationEventPublisher`
- Reutiliza `CharacterResponse`, `JobResponse` (já existem no master via auth-09)

## Links
- Issue pai: #15 (AUTH-10)
- Sub-issue TDD: #113
- Branch: `feature/auth-10-character-service`
