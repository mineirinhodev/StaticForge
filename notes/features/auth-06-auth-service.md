# AUTH-06: AuthService

**Branch**: feature/auth-06-auth-service
**Issue pai**: #11
**Sub-issue TDD**: #109

## Escopo
- `register`: criar usuário com senha BCrypt, gerar tokens
- `login`: validar credenciais, gerar tokens, controle de tentativas falhas
- `refresh`: validar refresh token por hash, revogar antigo, emitir novo par
- `logout`: revogar refresh token do usuário
- `forgotPassword` / `resetPassword`: gerar e validar token de reset (via e-mail — stub por ora)

## DTOs necessários
- `RegisterRequest`, `LoginRequest`, `RefreshRequest`, `ResetPasswordRequest`
- `AuthResponse` (accessToken + refreshToken)

## Exceções de domínio
- `EmailAlreadyExistsException` (409)
- `InvalidCredentialsException` (401)
- `TokenExpiredException` / `TokenRevokedException` (401)
- `AccountLockedException` (423)

## Decisões
- Refresh token armazenado como hash SHA-256 (nunca o valor plano)
- `isActive = true` ao registrar (sem e-mail de verificação por ora)
- Testes unitários com Mockito, sem contexto Spring
