# AUTH-13: GlobalExceptionHandler completo (RFC 7807)

## Escopo
- `GlobalExceptionHandler`: handlers para todos os tipos de erro da API
- `GlobalExceptionHandlerTest`: testa todos os 6 tipos com `ProblemDetail` content-type

## Handlers

| Exceção                     | Status HTTP | Detalhe               |
|-----------------------------|-------------|-----------------------|
| `MethodArgumentNotValidException` | 422    | campos inválidos      |
| `EmailAlreadyExistsException`     | 409    | mensagem da exceção   |
| `InvalidCredentialsException`     | 401    | mensagem da exceção   |
| `ResourceNotFoundException`       | 404    | mensagem da exceção   |
| `ForbiddenException`              | 403    | mensagem da exceção   |
| `IllegalStateException`           | 409    | mensagem da exceção   |
| `Exception` (catch-all)           | 500    | mensagem genérica (sem vazar detalhes internos) |

## Decisões
- Handler genérico usa mensagem fixa ("Ocorreu um erro interno...") para evitar information disclosure
- `GlobalExceptionHandlerTest` usa `@WebMvcTest(ExceptionTriggerController.class)` + `@Import` explícito
  da inner class pública (`public static`) para garantir registro pelo Spring MVC

## Links
- Issue pai: #17 (AUTH-13)
- Sub-issue TDD: #116 (fechada)
- Branch: `feature/auth-13-exception-handler`
- PR: #190
