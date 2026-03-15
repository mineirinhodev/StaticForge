# AUTH-04: JWT Infrastructure

GitHub Issue: #9

## Objetivo
Implementar `JwtProperties`, `JwtTokenProvider` e `JwtAuthenticationFilter`.

## Referência
- `docs/features/01-auth-profile.md` seção 2.4

## Pacote
`com.ffxiv.planner.auth.jwt`

## Componentes

### JwtProperties
- `@ConfigurationProperties(prefix = "app.jwt")`
- Campos: `secret` (String), `accessExpiration` (Duration), `refreshExpiration` (Duration)
- `@Validated` com `@NotBlank` / `@NotNull`

### JwtTokenProvider
- `@Component`
- Dependência: `JwtProperties`
- Métodos:
  - `generateAccessToken(UUID userId)` → String
  - `generateRefreshToken()` → String (UUID aleatório, não JWT)
  - `getUserIdFromToken(String token)` → UUID
  - `validateToken(String token)` → boolean
  - `getExpirationFromToken(String token)` → Instant

### JwtAuthenticationFilter
- Estende `OncePerRequestFilter`
- Extrai `Bearer <token>` do header `Authorization`
- Valida via `JwtTokenProvider`
- Seta `UsernamePasswordAuthenticationToken` no `SecurityContextHolder`
- Falha silenciosa (401 será emitido pelo SecurityConfig)

## Decisões
- Refresh token é UUID aleatório (não JWT) — hash SHA-256 armazenado no DB
- Access token é JWT assinado com HMAC-SHA256
- `JwtProperties` lida com `Duration` via Spring's duration parsing (`15m`, `30d`)
- Biblioteca: jjwt 0.12.6 (já no pom.xml)

## Config necessária (application.yml)
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    access-expiration: 15m
    refresh-expiration: 30d
```

## Estratégia de teste (TDD)
- Testes unitários para `JwtTokenProvider` (sem Spring context)
- Testar geração, validação, extração de userId, token expirado
- `JwtAuthenticationFilter` testado via MockMvc no AUTH-05 (SecurityConfig)

## Status
- [ ] RED: JwtTokenProviderTest
- [ ] GREEN: JwtProperties + JwtTokenProvider
- [ ] RED: JwtAuthenticationFilterTest
- [ ] GREEN: JwtAuthenticationFilter
- [ ] REFACTOR
