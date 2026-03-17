# PREMIUM-06: @RequiresPremium (Aspect)

Issue: https://github.com/mineirinhodev/StaticForge/issues/29
TDD Issue: https://github.com/mineirinhodev/StaticForge/issues/127

## Escopo
- Anotação `@RequiresPremium` (retention RUNTIME, target METHOD)
- `PremiumAspect` (@Around) que intercepta métodos anotados
  - Extrai UUID do `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`
  - Busca `User` via `UserRepository.findActiveById(uuid)`
  - Lança `ForbiddenException` se `!user.isPremium()`
- `ForbiddenException` já existe em `common.exception` → HTTP 403 via GlobalExceptionHandler

## Pacote
`com.ffxiv.planner.premium.aspect`

## Observações
- O principal no JWT filter é um `UUID` (não String)
- `ForbiddenException` já está mapeada no `GlobalExceptionHandler` → 403
- Sem AspectJ weaving customizado: usar `spring-boot-starter-aop` (já disponível via `spring-boot-starter`)
