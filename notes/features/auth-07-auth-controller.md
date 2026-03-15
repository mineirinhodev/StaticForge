# AUTH-07: AuthController

**Branch**: feature/auth-07-auth-controller
**Issue pai**: #12
**Sub-issue TDD**: #110

## Endpoints
- POST /auth/register → 201 Created + AuthResponse
- POST /auth/login → 200 OK + AuthResponse
- POST /auth/refresh → 200 OK + AuthResponse (cookie ou body)
- POST /auth/logout → 204 No Content
- POST /auth/forgot-password → 202 Accepted
- POST /auth/reset-password → 204 No Content

## DTOs adicionais necessários
- `RefreshRequest` (refreshToken: String)
- `ForgotPasswordRequest` (email: String)
- `ResetPasswordRequest` (token: String, newPassword: String)

## Estratégia de testes
- @WebMvcTest(AuthController.class)
- @MockBean AuthService
- Testar: status HTTP, corpo JSON, validação de campos obrigatórios (422)
- EmailAlreadyExistsException → 409
- InvalidCredentialsException → 401
