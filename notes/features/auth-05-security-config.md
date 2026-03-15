# AUTH-05: SecurityConfig

**Branch**: feature/auth-05-security-config
**Issue pai**: #10
**Sub-issue TDD**: #108

## Escopo
- `SecurityConfig`: SecurityFilterChain bean
- Rotas públicas: POST /api/v1/auth/register, POST /api/v1/auth/login, POST /api/v1/auth/refresh, POST /api/v1/auth/forgot-password, POST /api/v1/auth/reset-password
- Rotas protegidas: tudo mais exige JWT válido
- CSRF desabilitado (API stateless)
- Session management: STATELESS
- `JwtAuthenticationFilter` antes do `UsernamePasswordAuthenticationFilter`
- `PasswordEncoder` bean (BCryptPasswordEncoder)
- `AuthenticationManager` bean
- CORS: permitir frontend em dev, configurável em prod

## Decisões
- `@EnableWebSecurity` na `SecurityConfig`
- Usar `HttpSecurity.sessionManagement().sessionCreationPolicy(STATELESS)`
- `JwtAuthenticationEntryPoint` para retornar 401 JSON em vez de redirect HTML
- Testes com `@WebMvcTest` slice + `@Import(SecurityConfig.class)`

## Status
- [ ] RED: testes escritos
- [ ] GREEN: implementação
- [ ] REFACTOR
