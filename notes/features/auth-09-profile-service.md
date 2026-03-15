# AUTH-09: ProfileService e ProfileController

## Escopo
- `ProfileService`: `getMyProfile`, `getPublicProfile` (com cache Redis), `updateProfile` (com cache eviction)
- `ProfileController`: `GET /profile/me`, `PUT /profile/me`, `GET /users/{id}/profile`
- DTOs: `ProfileResponse`, `ProfileUpdateRequest`
- Cache: `profiles` com TTL 5min para perfis públicos

## Decisões
- Pacote `com.ffxiv.planner.profile` (package-by-feature)
- Reutiliza entidades de `com.ffxiv.planner.auth.entity`
- `getMyProfile` retorna todos os personagens e jobs do usuário autenticado
- `getPublicProfile` usa `@Cacheable("profiles")` — dados sem email/dados sensíveis
- Soft delete via `user.setDeletedAt(Instant.now())` + `revokeAllByUserId`
- `ProfileUpdatedEvent` publicado via `ApplicationEventPublisher`

## TDD Approach
Red → Green → Refactor, um teste por vez.
- `ProfileServiceTest`: Mockito, sem Spring context
- `ProfileControllerTest`: `@WebMvcTest(ProfileController.class)`

## Links
- Issue pai: #14 (AUTH-09)
- Sub-issue TDD: #112
- Branch: `feature/auth-09-profile-service`
