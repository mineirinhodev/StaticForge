# AUTH-08: OAuth 2.0 (Google + Discord)

**Branch**: feature/auth-08-oauth2
**Issue pai**: #13
**Sub-issue TDD**: #111

## Dependência adicionada
- spring-boot-starter-oauth2-client

## Componentes
- `OAuthService`: lógica criar/atualizar usuário OAuth (testável unitariamente)
  - processOAuthLogin(provider, providerId, email) → AuthResponse
  - Criar user se não existe; vincular por email se email já existe com outra conta
- `OAuth2LoginSuccessHandler`: intercepta sucesso OAuth, delega ao OAuthService, redireciona com token
- Configuração no `SecurityConfig`: adicionar `.oauth2Login()`

## Estratégia de testes
- OAuthServiceTest: Mockito puro (sem contexto Spring)
- OAuth2LoginSuccessHandlerTest: mock do OAuth2AuthenticationToken
- SecurityConfig já testado — só adicionar configuração

## Mapeamento de atributos
- Google: email = attributes["email"], providerId = sub
- Discord: email = attributes["email"], providerId = id
