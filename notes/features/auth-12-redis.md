# AUTH-12: Redis — configuração, cache e rate limiting

## Escopo
- `RedisConfig`: `RedisTemplate<String, Object>` com serialização JSON (Jackson), `RedisCacheManager` com TTL por cache
- `LoginRateLimiter`: limita tentativas de login por IP e por email usando contadores Redis
- Caches configurados: `profiles` (5min), `lodestone-verify` (1h)

## Decisões
- `RedisTemplate` com `StringRedisSerializer` para keys, `GenericJackson2JsonRedisSerializer` para values
- Rate limit: 5 tentativas em 15min por IP, 10 em 1h por email
- TTL por cache via `RedisCacheConfiguration` no `RedisCacheManager`
- Testes unitários do rate limiter com Mockito (sem Redis real)
- Smoke test já cobre context load com `@MockBean` para RedisConnectionFactory

## Links
- Issue pai: #17 (AUTH-12)
- Sub-issue TDD: #115
- Branch: `feature/auth-12-redis`
