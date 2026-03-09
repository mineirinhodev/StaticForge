# FFXIV Static Planner - Tasks de Desenvolvimento

Documento com todas as issues para criacao manual no GitHub.
Organizacao: issues granulares agrupadas por milestone (modulo).

---

## Labels

Criar as seguintes labels antes de criar as issues:

| Label             | Cor       | Descricao                      |
| ----------------- | --------- | ------------------------------ |
| `backend`         | `#0E8A16` | Trabalho no Spring Boot / Java |
| `frontend`        | `#1D76DB` | Trabalho no Next.js / React    |
| `database`        | `#FBCA04` | Migrations, schema, indices    |
| `infra`           | `#D93F0B` | Docker, CI/CD, deploy, config  |
| `docs`            | `#C5DEF5` | Documentacao                   |
| `auth`            | `#B60205` | Modulo de autenticacao         |
| `profile`         | `#E99695` | Modulo de perfil               |
| `static`          | `#0075CA` | Modulo de statics              |
| `matching`        | `#7057FF` | Modulo de matching             |
| `calendar`        | `#008672` | Modulo de calendario           |
| `reputation`      | `#D876E3` | Modulo de reputacao            |
| `chat`            | `#F9D0C4` | Modulo de chat                 |
| `premium`         | `#FEF2C0` | Modulo premium/pagamentos      |
| `priority:high`   | `#B60205` | Prioridade alta                |
| `priority:medium` | `#FBCA04` | Prioridade media               |
| `priority:low`    | `#0E8A16` | Prioridade baixa               |

---

## Milestone 0: Infraestrutura e Setup do Projeto

### INFRA-01: Inicializar monorepo com Turborepo

**Labels:** `infra`, `priority:high`

**Descricao:**
Configurar o monorepo com Turborepo 2.x e npm workspaces.

**Checklist:**

- [ ] Criar `package.json` root com workspaces
- [ ] Criar `turbo.json` com pipelines (dev, build, lint, test)
- [ ] Criar `apps/web/` (Next.js 15) com `create-next-app`
- [ ] Criar `apps/api/` (Spring Boot 3.3) com Spring Initializr
- [ ] Criar `packages/eslint-config/`
- [ ] Criar `packages/typescript-config/`
- [ ] Validar `npm run dev` roda ambos os apps
- [ ] Validar `npm run build` builda tudo via Turborepo

---

### INFRA-02: Configurar Docker Compose (dev)

**Labels:** `infra`, `priority:high`

**Descricao:**
Criar `docker-compose.yml` para ambiente de desenvolvimento local.

**Checklist:**

- [ ] PostgreSQL 16 Alpine com volume persistente
- [ ] Redis 7 Alpine
- [ ] Container do backend (Spring Boot) com hot reload
- [ ] Container do frontend (Next.js) com hot reload
- [ ] `.env.example` com todas as variaveis de ambiente
- [ ] `.dockerignore` para ambos os apps
- [ ] Documentar comandos no README

---

### INFRA-03: Configurar Dockerfiles de producao (multi-stage)

**Labels:** `infra`, `priority:medium`

**Descricao:**
Criar Dockerfiles multi-stage otimizados para producao.

**Checklist:**

- [ ] `apps/api/Dockerfile` (temurin:21-jdk build -> temurin:21-jre runtime)
- [ ] `apps/web/Dockerfile` (node:22 build -> standalone output)
- [ ] Nginx config para reverse proxy
- [ ] docker-compose.prod.yml

---

### INFRA-04: Configurar GitHub Actions (CI)

**Labels:** `infra`, `priority:medium`

**Descricao:**
Pipeline CI com lint, testes e build.

**Checklist:**

- [ ] Workflow `pr-validation.yml` (lint, test frontend, test backend, build)
- [ ] Workflow `staging-deploy.yml` (build Docker, push registry, deploy)
- [ ] Workflow `production-deploy.yml` (E2E, build prod images, deploy)
- [ ] Configurar services (PostgreSQL, Redis) nos workflows de teste

---

### INFRA-05: Configurar ESLint, Prettier e TypeScript compartilhados

**Labels:** `infra`, `frontend`, `priority:high`

**Descricao:**
Configuracoes compartilhadas via packages do monorepo.

**Checklist:**

- [ ] `packages/eslint-config/` com regras base
- [ ] `packages/typescript-config/` com tsconfig base
- [ ] `apps/web` estende as configs compartilhadas
- [ ] Prettier configurado no root
- [ ] Validar `npm run lint` funciona

---

## Milestone 1: Autenticacao & Perfil (Auth/Profile)

> **Depende de:** Milestone 0
> **Referencia:** `docs/features/01-auth-profile.md`

### AUTH-01: Criar migration V1 (users, characters, character_jobs, refresh_tokens)

**Labels:** `database`, `auth`, `profile`, `priority:high`

**Descricao:**
Criar a migration Flyway `V1__create_users_and_characters.sql`.

**Checklist:**

- [ ] Tabela `users` com todos os campos (email, password_hash, locale, is_premium, oauth, lockout, soft delete)
- [ ] Tabela `characters` com campos e FK para users
- [ ] Tabela `character_jobs` com FK para characters e constraint unique (character_id, job_name)
- [ ] Tabela `refresh_tokens` com FK para users
- [ ] Todos os indices definidos na feature doc
- [ ] Validar migration roda sem erros no PostgreSQL 16

---

### AUTH-02: Criar entities JPA (User, Character, CharacterJob, RefreshToken, AuditableEntity)

**Labels:** `backend`, `auth`, `profile`, `priority:high`

**Descricao:**
Implementar as entidades JPA com anotacoes, relacionamentos e enum Role.

**Checklist:**

- [ ] `AuditableEntity` (@MappedSuperclass com createdAt/updatedAt)
- [ ] `User` entity com todos os campos e relacionamentos
- [ ] `Character` entity com FK para User e lista de jobs
- [ ] `CharacterJob` entity com FK para Character e enum Role
- [ ] `RefreshToken` entity com FK para User
- [ ] Enum `Role` (TANK, HEALER, DPS)
- [ ] Configurar JPA Auditing (`@EnableJpaAuditing`)

---

### AUTH-03: Criar repositories (UserRepository, CharacterRepository, etc.)

**Labels:** `backend`, `auth`, `profile`, `priority:high`

**Descricao:**
Implementar interfaces Spring Data JPA com queries customizadas.

**Checklist:**

- [ ] `UserRepository` (findByEmail, findByOAuthProvider, findActiveById)
- [ ] `CharacterRepository` (findByUserId, findByLodestoneId, findMainByUserId, findLFSByDataCenter)
- [ ] `CharacterJobRepository` (findByCharacterId, findMainByCharacterId)
- [ ] `RefreshTokenRepository` (findByTokenHash, revokeAllByUserId, deleteExpired)

---

### AUTH-04: Implementar JWT (JwtTokenProvider, JwtAuthenticationFilter, JwtProperties)

**Labels:** `backend`, `auth`, `priority:high`

**Descricao:**
Implementar infraestrutura JWT completa.

**Checklist:**

- [ ] `JwtProperties` (@ConfigurationProperties com secret, accessExpiration, refreshExpiration)
- [ ] `JwtTokenProvider` (generateAccessToken, generateRefreshToken, getUserIdFromToken, validateToken)
- [ ] `JwtAuthenticationFilter` (OncePerRequestFilter - extrai token do header, valida, seta SecurityContext)
- [ ] Configurar jjwt-api/impl/jackson no build.gradle.kts
- [ ] Testes unitarios do JwtTokenProvider

---

### AUTH-05: Configurar Spring Security (SecurityConfig)

**Labels:** `backend`, `auth`, `priority:high`

**Descricao:**
Configuracao central de seguranca com JWT.

**Checklist:**

- [ ] `SecurityConfig` com HttpSecurity configurado
- [ ] Endpoints publicos: register, login, refresh, forgot-password, reset-password, perfis publicos
- [ ] Endpoints autenticados: todos os demais
- [ ] CSRF desabilitado (API stateless)
- [ ] CORS configurado (`CorsConfig`)
- [ ] Registrar JwtAuthenticationFilter antes do UsernamePasswordAuthenticationFilter
- [ ] BCrypt PasswordEncoder bean

---

### AUTH-06: Implementar AuthService (register, login, refresh, logout, forgot/reset password)

**Labels:** `backend`, `auth`, `priority:high`

**Descricao:**
Servico de autenticacao completo.

**Checklist:**

- [ ] `register()` -- validacao de email unico, hash BCrypt, evento UserRegisteredEvent
- [ ] `login()` -- verificacao de lockout (5 tentativas = 15min), validacao de senha, geracao de tokens
- [ ] `refresh()` -- refresh token rotation, geracao de novo par de tokens
- [ ] `logout()` -- revogacao de refresh token, limpeza de cookie
- [ ] `forgotPassword()` -- geracao de token de reset, armazenamento no Redis com TTL 1h
- [ ] `resetPassword()` -- validacao de token, atualizacao de senha, revogacao de todos os refresh tokens
- [ ] Testes unitarios com Mockito

---

### AUTH-07: Implementar AuthController

**Labels:** `backend`, `auth`, `priority:high`

**Descricao:**
Controller REST para endpoints de autenticacao.

**Checklist:**

- [ ] POST `/api/v1/auth/register` -> 201
- [ ] POST `/api/v1/auth/login` -> 200 (seta cookie httpOnly com refresh token)
- [ ] POST `/api/v1/auth/refresh` -> 200 (le cookie, rotaciona token)
- [ ] POST `/api/v1/auth/logout` -> 204
- [ ] POST `/api/v1/auth/forgot-password` -> 200
- [ ] POST `/api/v1/auth/reset-password` -> 200
- [ ] Request/Response DTOs (RegisterRequest, LoginRequest, LoginResponse, etc.)
- [ ] Bean Validation nos DTOs (@NotBlank, @Email, @Size, @Pattern)
- [ ] Testes de integracao com MockMvc

---

### AUTH-08: Implementar OAuth 2.0 (Google + Discord)

**Labels:** `backend`, `auth`, `priority:medium`

**Descricao:**
Autenticacao via provedores OAuth 2.0.

**Checklist:**

- [ ] `CustomOAuth2UserService` (cria/vincula usuario)
- [ ] `OAuth2SuccessHandler` (gera JWT e redireciona para frontend)
- [ ] Configuracao no application.yml (client-id, client-secret, redirect-uri)
- [ ] Suporte a Google e Discord como provedores
- [ ] Testes

---

### AUTH-09: Implementar ProfileService e ProfileController

**Labels:** `backend`, `profile`, `priority:high`

**Descricao:**
CRUD de perfil do usuario.

**Checklist:**

- [ ] `ProfileService.getMyProfile()` -- perfil completo com characters e jobs
- [ ] `ProfileService.getPublicProfile()` -- perfil publico com cache Redis (5min)
- [ ] `ProfileService.updateProfile()` -- atualiza locale, bio, availability; invalida cache
- [ ] `ProfileController` com endpoints GET/PUT `/profile/me` e GET `/users/{id}/profile`
- [ ] MapStruct mappers (UserMapper, CharacterMapper)
- [ ] Evento ProfileUpdatedEvent
- [ ] Testes

---

### AUTH-10: Implementar CharacterService e CharacterController

**Labels:** `backend`, `profile`, `priority:high`

**Descricao:**
CRUD de personagens com logica de main character.

**Checklist:**

- [ ] `createCharacter()` -- logica de isMain (primeiro = main automatico)
- [ ] `updateCharacter()` -- verificacao de ownership
- [ ] `deleteCharacter()` -- nao pode ser o unico, promove outro a main se necessario
- [ ] CharacterController com endpoints POST/PUT/DELETE `/characters`
- [ ] CharacterJobService (addJob, updateJob, deleteJob) com endpoints
- [ ] MapStruct mappers (CharacterMapper, JobMapper)
- [ ] Testes

---

### AUTH-11: Implementar verificacao Lodestone (LodestoneClient + fluxo de verificacao)

**Labels:** `backend`, `profile`, `priority:medium`

**Descricao:**
Integracao com XIVAPI para verificacao de personagens.

**Checklist:**

- [ ] `LodestoneClient` (WebFlux) -- buscar perfil por nome+server, buscar por lodestoneId
- [ ] `LodestoneMapper` -- mapear response da API para dados do character
- [ ] `CharacterService.startVerification()` -- gera codigo, armazena no Redis, retorna instrucoes
- [ ] `CharacterService.confirmVerification()` -- verifica codigo na bio, importa dados, marca verificado
- [ ] Endpoints POST `/characters/{id}/verify` e POST `/characters/{id}/verify/confirm`
- [ ] Cache Redis: `lodestone:{lodestoneId}` TTL 1h
- [ ] Testes (mock da API externa)

---

### AUTH-12: Configurar Redis (cache, sessoes, rate limiting)

**Labels:** `backend`, `infra`, `auth`, `priority:high`

**Descricao:**
Configuracao do Redis para cache, sessoes JWT e rate limiting.

**Checklist:**

- [ ] `RedisConfig` (RedisTemplate, CacheManager)
- [ ] Configuracao de cache com TTLs por tipo (profiles 5min, lodestone 1h)
- [ ] Rate limiting para login (sliding window counter)
- [ ] Armazenamento de refresh token hash em `session:{userId}`
- [ ] Armazenamento de tokens de verificacao e reset

---

### AUTH-13: Implementar GlobalExceptionHandler (RFC 7807)

**Labels:** `backend`, `priority:high`

**Descricao:**
Handler global de excecoes com formato RFC 7807 Problem Details.

**Checklist:**

- [ ] `GlobalExceptionHandler` (@RestControllerAdvice)
- [ ] `BusinessException` (base para excecoes de negocio)
- [ ] `ResourceNotFoundException` (404)
- [ ] `ApiError` DTO (RFC 7807 format)
- [ ] Handlers para: ValidationException (400), AuthenticationException (401), AccessDeniedException (403), ResourceNotFoundException (404), BusinessException (409/422), RateLimitException (429)
- [ ] Testes

---

### AUTH-14: Frontend -- Paginas de autenticacao (Login, Register, Forgot Password)

**Labels:** `frontend`, `auth`, `priority:high`

**Descricao:**
Implementar paginas de autenticacao com formularios validados.

**Checklist:**

- [ ] `/[locale]/(auth)/login/page.tsx` -- formulario de login + botoes OAuth
- [ ] `/[locale]/(auth)/register/page.tsx` -- formulario de registro
- [ ] `/[locale]/(auth)/forgot-password/page.tsx` -- solicitar reset
- [ ] `/[locale]/(auth)/reset-password/page.tsx` -- redefinir senha (com token na URL)
- [ ] Schemas Zod: `loginSchema`, `registerSchema`, `forgotPasswordSchema`, `resetPasswordSchema`
- [ ] React Hook Form integrado com Zod
- [ ] Componentes ShadCN/UI (Input, Button, Card, Form)

---

### AUTH-15: Frontend -- Zustand auth store e Axios interceptors

**Labels:** `frontend`, `auth`, `priority:high`

**Descricao:**
Gerenciamento de estado de autenticacao e cliente HTTP.

**Checklist:**

- [ ] `stores/auth-store.ts` (Zustand -- accessToken, user, isAuthenticated, setAuth, clearAuth)
- [ ] `lib/api.ts` (Axios instance com base URL)
- [ ] Request interceptor: injeta Authorization Bearer header
- [ ] Response interceptor: refresh automatico em 401, redirect para login se refresh falha
- [ ] `hooks/use-auth.ts` (useLogin, useRegister, useLogout, useRefreshToken)

---

### AUTH-16: Frontend -- Paginas de perfil e gerenciamento de personagens

**Labels:** `frontend`, `profile`, `priority:high`

**Descricao:**
Paginas de perfil, edicao e gerenciamento de characters/jobs.

**Checklist:**

- [ ] `/[locale]/(main)/profile/page.tsx` -- meu perfil
- [ ] `/[locale]/(main)/profile/edit/page.tsx` -- editar perfil
- [ ] `/[locale]/(main)/profile/characters/page.tsx` -- CRUD de characters e jobs
- [ ] Componentes: ProfileHeader, CharacterCard, JobBadge, CharacterForm, JobForm
- [ ] Fluxo de verificacao Lodestone (VerificationCodeDisplay, ConfirmButton)
- [ ] `hooks/use-profile.ts`, `hooks/use-characters.ts`
- [ ] Schemas Zod: `profileSchema`, `characterSchema`, `jobSchema`

---

### AUTH-17: Frontend -- Configurar i18n (next-intl)

**Labels:** `frontend`, `infra`, `priority:high`

**Descricao:**
Setup de internacionalizacao com next-intl.

**Checklist:**

- [ ] Instalar e configurar `next-intl` com App Router
- [ ] Middleware de deteccao de locale e redirect
- [ ] Segmento `[locale]` no App Router
- [ ] Arquivos de traducao iniciais: `messages/en.json`, `messages/pt.json`
- [ ] Namespaces: `auth.*`, `profile.*`, `character.*`, `validation.*`, `ffxiv.*`
- [ ] Componente `LocaleSwitcher` no header

---

### AUTH-18: Frontend -- Layout base (Header, Sidebar, Footer, middleware de auth)

**Labels:** `frontend`, `infra`, `priority:high`

**Descricao:**
Layout compartilhado e middleware de protecao de rotas.

**Checklist:**

- [ ] `app/[locale]/layout.tsx` -- root layout com providers (Zustand, TanStack Query, next-intl)
- [ ] `app/[locale]/(auth)/layout.tsx` -- layout de autenticacao (centralizado, sem sidebar)
- [ ] `app/[locale]/(main)/layout.tsx` -- layout autenticado (com header e sidebar)
- [ ] Componentes: Header (logo, nav, user menu, locale switcher), Sidebar (nav links), Footer
- [ ] `middleware.ts` -- redirect para login se nao autenticado em rotas protegidas
- [ ] Configurar Tailwind CSS 4 e ShadCN/UI
- [ ] Dark mode (via Tailwind + next-themes)

---

## Milestone 2: Premium e Assinaturas

> **Depende de:** Milestone 1
> **Referencia:** `docs/features/07-premium.md`

### PREMIUM-01: Criar migration V8 (subscriptions, payment_history)

**Labels:** `database`, `premium`, `priority:high`

**Descricao:**
Migration Flyway para tabelas de assinaturas e pagamentos.

**Checklist:**

- [ ] Tabela `subscriptions` com todos os campos e FK para users
- [ ] Tabela `payment_history` com FK para subscriptions
- [ ] Indices conforme feature doc
- [ ] Validar migration

---

### PREMIUM-02: Criar entities JPA (Subscription, PaymentHistory) e enums

**Labels:** `backend`, `premium`, `priority:high`

**Checklist:**

- [ ] `Subscription` entity
- [ ] `PaymentHistory` entity
- [ ] Enums: `SubscriptionPlan`, `SubscriptionStatus`, `PaymentStatus`
- [ ] Repositories com queries customizadas

---

### PREMIUM-03: Implementar StripeService (integracao Stripe)

**Labels:** `backend`, `premium`, `priority:high`

**Checklist:**

- [ ] Adicionar `stripe-java` ao build.gradle.kts
- [ ] `StripeService` (getOrCreateCustomer, createCheckoutSession, cancelSubscription, reactivateSubscription, constructWebhookEvent)
- [ ] Configuracao de chaves Stripe no application.yml
- [ ] Testes com mocks do Stripe

---

### PREMIUM-04: Implementar SubscriptionService

**Labels:** `backend`, `premium`, `priority:high`

**Checklist:**

- [ ] `getAvailablePlans()` -- lista planos com precos
- [ ] `createSubscription()` -- cria checkout session no Stripe
- [ ] `getMySubscription()` -- retorna assinatura atual
- [ ] `cancelSubscription()` -- cancel_at_period_end (RN43)
- [ ] `reactivateSubscription()` -- remove cancelamento
- [ ] `activateTrial()` -- trial 7 dias, unico por conta (RN45)
- [ ] `expireSubscription()` -- downgrade (RN44)
- [ ] Eventos: SubscriptionActivatedEvent, SubscriptionExpiredEvent, etc.
- [ ] Testes

---

### PREMIUM-05: Implementar StripeWebhookController

**Labels:** `backend`, `premium`, `priority:high`

**Checklist:**

- [ ] POST `/api/v1/webhooks/stripe` -- recebe e verifica webhooks
- [ ] Handler `checkout.session.completed` -- cria Subscription, ativa premium
- [ ] Handler `invoice.paid` -- renova periodo, registra pagamento
- [ ] Handler `invoice.payment_failed` -- marca PAST_DUE, notifica usuario
- [ ] Handler `customer.subscription.deleted` -- expira assinatura
- [ ] Idempotencia via stripe_payment_id unique
- [ ] Testes

---

### PREMIUM-06: Implementar @RequiresPremium (Aspect)

**Labels:** `backend`, `premium`, `priority:high`

**Checklist:**

- [ ] Annotation `@RequiresPremium`
- [ ] `PremiumAspect` (@Around) -- verifica isPremium, lanca PremiumRequiredException se false
- [ ] Testes

---

### PREMIUM-07: Implementar SubscriptionController e DTOs

**Labels:** `backend`, `premium`, `priority:high`

**Checklist:**

- [ ] DTOs: CreateSubscriptionRequest, PlanResponse, SubscriptionResponse, CheckoutSessionResponse, PaymentHistoryResponse
- [ ] SubscriptionMapper (MapStruct)
- [ ] SubscriptionController com todos os endpoints
- [ ] Testes de integracao

---

### PREMIUM-08: Implementar SubscriptionScheduler (job diario)

**Labels:** `backend`, `premium`, `priority:medium`

**Checklist:**

- [ ] Quartz job diario as 01:00 UTC
- [ ] Expirar assinaturas ACTIVE/TRIALING vencidas
- [ ] Expirar PAST_DUE com mais de 7 dias
- [ ] Alertar assinaturas expirando em 3 dias
- [ ] Atualizar CANCELED com endDate passado para EXPIRED

---

### PREMIUM-09: Frontend -- Pagina de planos premium

**Labels:** `frontend`, `premium`, `priority:high`

**Checklist:**

- [ ] `/[locale]/(main)/premium/page.tsx`
- [ ] Componentes: PlanComparison, PlanCard, PriceDisplay, FeatureList, SubscribeButton
- [ ] Hook `usePlans()`, `useCreateSubscription()`
- [ ] Redirect para Stripe Checkout

---

### PREMIUM-10: Frontend -- Pagina de gerenciamento de assinatura

**Labels:** `frontend`, `premium`, `priority:high`

**Checklist:**

- [ ] `/[locale]/(main)/account/subscription/page.tsx`
- [ ] Componentes: SubscriptionStatus, CancelButton, ReactivateButton, PaymentHistoryTable
- [ ] `/[locale]/(main)/account/settings/page.tsx`
- [ ] Hooks: `useMySubscription()`, `useCancelSubscription()`, `usePaymentHistory()`
- [ ] Trial banner com `useActivateTrial()`

---

## Milestone 3: Gestao de Statics

> **Depende de:** Milestone 1, Milestone 2
> **Referencia:** `docs/features/02-static-management.md`

### STATIC-01: Criar migrations V2 e V3 (statics, memberships, vacancies, applications, invitations)

**Labels:** `database`, `static`, `priority:high`

**Checklist:**

- [ ] `V2__create_statics_and_memberships.sql` (statics, static_memberships)
- [ ] `V3__create_vacancies_applications_invitations.sql` (static_vacancies, applications, invitations)
- [ ] Extensao `pg_trgm` para busca textual por nome de static
- [ ] Todos os indices (incluindo GIN trigram)
- [ ] Validar migrations

---

### STATIC-02: Criar entities JPA (Static, StaticMembership, StaticVacancy, Application, Invitation)

**Labels:** `backend`, `static`, `priority:high`

**Checklist:**

- [ ] `Static` entity com enums (StaticStatus)
- [ ] `StaticMembership` entity
- [ ] `StaticVacancy` entity com JSONB fields
- [ ] `Application` entity com enum ApplicationStatus
- [ ] `Invitation` entity com enum InvitationStatus
- [ ] Todos os relacionamentos JPA

---

### STATIC-03: Criar repositories com queries e Specifications

**Labels:** `backend`, `static`, `priority:high`

**Checklist:**

- [ ] `StaticRepository` com findActiveById, findByMemberUserId, countByLeaderUserId
- [ ] `StaticSpecifications` para busca dinamica (filtros por DC, role, language, status, nome)
- [ ] `StaticMembershipRepository` (findActiveByStaticId, findLeaderByStaticId, countActive)
- [ ] `StaticVacancyRepository` (findByStaticIdAndIsOpenTrue)
- [ ] `ApplicationRepository` (findPendingByStaticId, findByUserId, findExpired)
- [ ] `InvitationRepository` (findPendingByUserId, findExpired)

---

### STATIC-04: Implementar StaticService (CRUD, gestao de membros)

**Labels:** `backend`, `static`, `priority:high`

**Checklist:**

- [ ] `createStatic()` -- verificacao de limite free/premium (RN10), cria membership de lider
- [ ] `updateStatic()` -- verificacao de lideranca
- [ ] `dissolveStatic()` -- soft delete, notificacao de membros (RN12)
- [ ] `getMembers()`, `updateMember()`, `removeMember()`
- [ ] `transferLeadership()` (RN07)
- [ ] `leaveStatic()` -- lider deve transferir antes
- [ ] Eventos: StaticCreatedEvent, StaticDissolvedEvent, MemberJoinedEvent, MemberLeftEvent
- [ ] Testes

---

### STATIC-05: Implementar VacancyService

**Labels:** `backend`, `static`, `priority:high`

**Checklist:**

- [ ] `createVacancy()` -- verificacao de lideranca, espaco na static
- [ ] `updateVacancy()`, `closeVacancy()`
- [ ] `getVacancies()`
- [ ] Evento VacancyCreatedEvent
- [ ] Testes

---

### STATIC-06: Implementar ApplicationService

**Labels:** `backend`, `static`, `priority:high`

**Checklist:**

- [ ] `apply()` -- vaga aberta, character verificado, sem duplicata, nao membro (RN14)
- [ ] `acceptApplication()` -- cria membership, fecha vaga, rejeita demais candidaturas
- [ ] `rejectApplication()`, `withdrawApplication()`
- [ ] `getStaticApplications()`, `getMyApplications()`
- [ ] Eventos: ApplicationCreatedEvent, ApplicationAcceptedEvent
- [ ] Testes

---

### STATIC-07: Implementar InvitationService

**Labels:** `backend`, `static`, `priority:high`

**Checklist:**

- [ ] `invite()` -- verificacao de lideranca, character verificado, sem duplicata (RN15)
- [ ] `acceptInvitation()` -- cria membership, fecha vaga se aplicavel
- [ ] `rejectInvitation()`, `cancelInvitation()`
- [ ] `getMyInvitations()`
- [ ] Eventos: InvitationCreatedEvent, InvitationAcceptedEvent
- [ ] Testes

---

### STATIC-08: Implementar SearchService (busca de statics e jogadores)

**Labels:** `backend`, `static`, `priority:medium`

**Descricao:**
Busca com filtros dinamicos via Specifications.

**Checklist:**

- [ ] `searchStatics()` -- filtros por DC, server, role, language, contentScope, status, query
- [ ] `searchPlayers()` -- filtros por DC, role, job, parseTier, lookingForStatic
- [ ] SearchController com GET `/search/statics` e `/search/players`
- [ ] Paginacao e ordenacao
- [ ] Testes

---

### STATIC-09: Implementar Controllers (Static, Vacancy, Application, Invitation, Search)

**Labels:** `backend`, `static`, `priority:high`

**Checklist:**

- [ ] `StaticController` -- CRUD, membros, transferencia, leave
- [ ] `VacancyController` -- CRUD de vagas
- [ ] `ApplicationController` -- aplicar, aceitar, rejeitar, cancelar, listar
- [ ] `InvitationController` -- convidar, aceitar, rejeitar, cancelar, listar
- [ ] `SearchController` -- buscar statics e jogadores
- [ ] Todos os DTOs e MapStruct mappers
- [ ] Testes de integracao

---

### STATIC-10: Implementar ExpirationJob (expirar candidaturas e convites)

**Labels:** `backend`, `static`, `priority:medium`

**Checklist:**

- [ ] Quartz job diario as 00:00 UTC
- [ ] Expirar candidaturas com mais de 14 dias (RN17)
- [ ] Expirar convites com mais de 7 dias (RN18)
- [ ] Notificar envolvidos
- [ ] Testes

---

### STATIC-11: Frontend -- Paginas de statics (listar, criar, ver, gerenciar)

**Labels:** `frontend`, `static`, `priority:high`

**Checklist:**

- [ ] `/statics` -- listar minhas statics
- [ ] `/statics/create` -- formulario de criacao com Zod
- [ ] `/statics/[id]` -- pagina publica da static (SSR + ISR)
- [ ] `/statics/[id]/manage` -- painel de gestao (membros, configuracoes, dissolver)
- [ ] `/statics/[id]/applications` -- listar e gerenciar candidaturas
- [ ] Componentes: StaticCard, MemberList, VacancyForm, ApplicationCard, InvitationDialog
- [ ] Hooks: useMyStatics, useStatic, useCreateStatic, useDissolveStatic

---

### STATIC-12: Frontend -- Paginas de busca (statics e jogadores)

**Labels:** `frontend`, `static`, `priority:high`

**Checklist:**

- [ ] `/search/statics` -- busca com filtros e paginacao (URL state com nuqs)
- [ ] `/search/players` -- busca com filtros e paginacao
- [ ] Componentes: SearchFilters, StaticResultList, PlayerResultList, Pagination
- [ ] Hooks: useSearchStatics, useSearchPlayers
- [ ] Schemas Zod para filtros

---

## Milestone 4: Matching

> **Depende de:** Milestone 1, Milestone 3
> **Referencia:** `docs/features/03-matching.md`

### MATCH-01: Criar migration V4 (match_suggestions, match_preferences)

**Labels:** `database`, `matching`, `priority:high`

**Checklist:**

- [ ] Tabelas com campos JSONB para criteria e weights
- [ ] Indices para busca por character, status e score
- [ ] Validar migration

---

### MATCH-02: Criar entities e repositories

**Labels:** `backend`, `matching`, `priority:high`

**Checklist:**

- [ ] `MatchSuggestion` entity com enums (MatchTargetType, MatchStatus)
- [ ] `MatchPreference` entity com campos JSONB
- [ ] Repositories com queries customizadas

---

### MATCH-03: Implementar MatchCalculationService (algoritmo de scoring)

**Labels:** `backend`, `matching`, `priority:high`

**Descricao:**
Implementar o algoritmo de matching com 7 criterios ponderados.

**Checklist:**

- [ ] Criterio 1: Role/Job (25%) -- match exato, match de role, sem match
- [ ] Criterio 2: Schedule (25%) -- calculo de sobreposicao horaria
- [ ] Criterio 3: Data Center (15%) -- mesmo server, mesmo DC, mesma regiao (RN23)
- [ ] Criterio 4: Language (10%) -- match de idioma
- [ ] Criterio 5: Progression (10%) -- nivel de progressao
- [ ] Criterio 6: Content Scope (10%) -- tipos de conteudo
- [ ] Criterio 7: Playstyle (5%) -- seriedade e parse tier
- [ ] Score final como soma ponderada, threshold 40% (RN22)
- [ ] Testes extensivos do algoritmo com cenarios variados

---

### MATCH-04: Implementar MatchingService e MatchScheduler

**Labels:** `backend`, `matching`, `priority:high`

**Checklist:**

- [ ] `getMatches()` -- limite free 5/semana (RN20), threshold 40% (RN22)
- [ ] `getMatch()`, `dismissMatch()`
- [ ] `getPreferences()`, `updatePreferences()` -- pesos devem somar 100 (RN21)
- [ ] `MatchScheduler` -- job diario 03:00 UTC com lock distribuido Redis
- [ ] Consumir eventos: CharacterVerifiedEvent, ProfileUpdatedEvent, VacancyCreatedEvent
- [ ] Cache Redis: `match:{characterId}` 24h, `count:matches:{userId}:{week}` 7d
- [ ] Testes

---

### MATCH-05: Implementar MatchController e DTOs

**Labels:** `backend`, `matching`, `priority:high`

**Checklist:**

- [ ] Controller com endpoints GET/PUT /matches, GET/PUT /matches/preferences
- [ ] DTOs: MatchSuggestionResponse, MatchCriteriaBreakdown, MatchPreferencesResponse
- [ ] MatchMapper (MapStruct)
- [ ] Testes

---

### MATCH-06: Frontend -- Paginas de matching

**Labels:** `frontend`, `matching`, `priority:high`

**Checklist:**

- [ ] `/matches` -- lista de sugestoes com score e preview
- [ ] `/matches/preferences` -- editor de pesos (sliders somam 100), filtros, blacklist
- [ ] Componentes: MatchCard, CompatibilityScore, CompatibilityBreakdown (grafico radar), PreferencesForm
- [ ] Hooks: useMatches, useMatch, useDismissMatch, useMatchPreferences
- [ ] Schema Zod: matchPreferencesSchema (refine: pesos somam 100)
- [ ] WebSocket subscription para `/user/queue/matches`

---

## Milestone 5: Calendario

> **Depende de:** Milestone 3
> **Referencia:** `docs/features/04-calendar.md`

### CAL-01: Criar migration V5 (raid_events, attendances)

**Labels:** `database`, `calendar`, `priority:high`

**Checklist:**

- [ ] Tabelas com suporte a eventos recorrentes (recurrence_rule) e PUG (is_public)
- [ ] Indices para busca por static, datas e PUGs publicos
- [ ] Validar migration

---

### CAL-02: Criar entities, repositories e DTOs

**Labels:** `backend`, `calendar`, `priority:high`

**Checklist:**

- [ ] `RaidEvent` entity com enum AttendanceStatus
- [ ] `Attendance` entity com constraint unique (event_id, character_id)
- [ ] Repositories com queries para static events, public events, user calendar, reminders
- [ ] DTOs: CreateEventRequest, EventResponse, AttendanceResponse, CalendarResponse
- [ ] EventMapper (MapStruct)

---

### CAL-03: Implementar EventService e AttendanceService

**Labels:** `backend`, `calendar`, `priority:high`

**Checklist:**

- [ ] `createStaticEvent()` -- verificacao de lideranca, cria attendances TENTATIVE (RN24)
- [ ] `createPugEvent()` -- @RequiresPremium, maxParticipants e dataCenter obrigatorios (RN26)
- [ ] `updateEvent()`, `cancelEvent()`
- [ ] `getStaticEvents()`, `getPublicEvents()`, `getMyCalendar()`
- [ ] `updateAttendance()` -- notifica lider se ABSENT (RN25, RN29)
- [ ] `registerForPug()` -- vagas limitadas, waitlist (RN27)
- [ ] `unregisterFromPug()` -- promove waitlisted
- [ ] Consumir eventos: StaticDissolvedEvent, MemberJoinedEvent, MemberLeftEvent
- [ ] WebSocket broadcast via `/topic/static.{staticId}`
- [ ] Testes

---

### CAL-04: Implementar iCalService e exportacao

**Labels:** `backend`, `calendar`, `priority:medium`

**Checklist:**

- [ ] `ICalService.generateIcal()` -- formato RFC 5545 (VCALENDAR, VEVENT, RRULE, VALARM)
- [ ] GET `/calendar/me/ical` -- @RequiresPremium, retorna text/calendar
- [ ] Testes

---

### CAL-05: Implementar ReminderScheduler

**Labels:** `backend`, `calendar`, `priority:medium`

**Checklist:**

- [ ] Job a cada 15 minutos
- [ ] Lembrete 24h antes e 1h antes (RN28)
- [ ] Evitar envio duplicado (verificar notificacoes ja enviadas)
- [ ] Premium: lembretes customizaveis
- [ ] Testes

---

### CAL-06: Implementar EventController

**Labels:** `backend`, `calendar`, `priority:high`

**Checklist:**

- [ ] Todos os endpoints REST conforme feature doc
- [ ] Testes de integracao

---

### CAL-07: Frontend -- Paginas de calendario

**Labels:** `frontend`, `calendar`, `priority:high`

**Checklist:**

- [ ] `/calendar` -- FullCalendar wrapper com modos mes/semana/dia
- [ ] `/calendar/events/[id]` -- detalhe do evento com painel de presencas
- [ ] `/calendar/events/create` -- formulario com DateTimePicker, RecurrenceEditor, PugOptions
- [ ] `/calendar/public` -- lista de eventos PUG por Data Center
- [ ] Componentes: CalendarView, EventForm, AttendancePanel, PugEventCard
- [ ] Hooks: useMyCalendar, useStaticEvents, usePublicEvents, useCreateEvent, useUpdateAttendance
- [ ] WebSocket subscription para `/topic/static.{staticId}` (event updates)
- [ ] Instalar e configurar FullCalendar (React wrapper)

---

## Milestone 6: Reputacao

> **Depende de:** Milestone 1, Milestone 3
> **Referencia:** `docs/features/05-reputation.md`

### REP-01: Criar migration V6 (reviews, review_reports)

**Labels:** `database`, `reputation`, `priority:high`

**Checklist:**

- [ ] Tabelas com CHECK constraints (scores 1-5) e UNIQUEs
- [ ] Indices para busca por target, reviewer e reports pendentes
- [ ] Validar migration

---

### REP-02: Criar entities, repositories e DTOs

**Labels:** `backend`, `reputation`, `priority:high`

**Checklist:**

- [ ] `Review` entity com enums (ReviewTargetType) e soft delete
- [ ] `ReviewReport` entity com enums (ReportReason, ReportStatus)
- [ ] Repositories com queries para calcular media, contar reviews, buscar por target
- [ ] DTOs e ReviewMapper

---

### REP-03: Implementar ReviewService e CoexistenceValidator

**Labels:** `backend`, `reputation`, `priority:high`

**Checklist:**

- [ ] `CoexistenceValidator` -- valida 2 semanas de convivencia na mesma static (RN32)
- [ ] `createReview()` -- character verificado, mesma static, 2 semanas, sem auto-review, sem duplicata (RN30-RN33)
- [ ] `updateReview()` -- apenas autor
- [ ] `getPlayerReviews()`, `getStaticReviews()`
- [ ] Evento ReviewCreatedEvent
- [ ] Verificacao de reputacao abaixo de 2.0 (RN36)
- [ ] Testes

---

### REP-04: Implementar ReputationService (calculo de media ponderada temporal)

**Labels:** `backend`, `reputation`, `priority:high`

**Checklist:**

- [ ] `getReputation()` -- media ponderada com peso temporal (RN35): peso = 1 / (1 + dias/180)
- [ ] Breakdown por criterio (pontualidade, comunicacao, performance, atitude, comprometimento)
- [ ] Calculo de tendencia (comparacao ultimo mes vs anterior)
- [ ] Cache Redis: `reputation:{targetId}:{type}` TTL 10min
- [ ] `isBelowThreshold()` -- usado pelo MatchingService (RN36)
- [ ] Testes do algoritmo de media ponderada

---

### REP-05: Implementar ReviewReportService e Controller

**Labels:** `backend`, `reputation`, `priority:medium`

**Checklist:**

- [ ] `reportReview()` -- sem auto-report, sem duplicata, alerta se 3+ reports (RN34)
- [ ] Controller com todos os endpoints (publicos para GET, autenticados para POST/PUT)
- [ ] Testes

---

### REP-06: Frontend -- Componentes de reputacao

**Labels:** `frontend`, `reputation`, `priority:high`

**Checklist:**

- [ ] Componentes: ReputationCard (score circular), ReputationChart (radar Recharts), ReviewList, ReviewCard, ReviewForm (5 sliders), ReportDialog
- [ ] Integrar na pagina de perfil (`/users/[id]`) e pagina de static (`/statics/[id]`)
- [ ] Hooks: useReputation, usePlayerReviews, useStaticReviews, useCreateReview, useReportReview
- [ ] Schema Zod: createReviewSchema, createReportSchema
- [ ] Instalar e configurar Recharts

---

## Milestone 7: Chat

> **Depende de:** Milestone 1, Milestone 3
> **Referencia:** `docs/features/06-chat.md`

### CHAT-01: Criar migration V7 (chat_rooms, chat_room_members, messages, message_reads)

**Labels:** `database`, `chat`, `priority:high`

**Checklist:**

- [ ] Tabelas com UNIQUEs e suporte a soft delete de mensagens
- [ ] Indices para busca por room, user, sent_at
- [ ] Validar migration

---

### CHAT-02: Criar entities, repositories e DTOs

**Labels:** `backend`, `chat`, `priority:high`

**Checklist:**

- [ ] `ChatRoom` entity com enum ChatRoomType (DM, STATIC)
- [ ] `ChatRoomMember`, `Message`, `MessageRead` entities
- [ ] Repositories com queries para rooms do usuario, DM entre dois users, mensagens paginadas, unread count
- [ ] DTOs e ChatMapper

---

### CHAT-03: Configurar WebSocket (Spring + STOMP + SockJS)

**Labels:** `backend`, `chat`, `infra`, `priority:high`

**Checklist:**

- [ ] `WebSocketConfig` -- registrar endpoint `/ws`, configurar STOMP broker
- [ ] `ChannelInterceptor` -- validar JWT no CONNECT frame
- [ ] Configurar origins permitidas para WebSocket
- [ ] Testar conexao basica com cliente STOMP

---

### CHAT-04: Implementar ChatService e MessageService

**Labels:** `backend`, `chat`, `priority:high`

**Checklist:**

- [ ] `getMyChatRooms()` -- lista com ultimo mensagem e unread count
- [ ] `createDm()` -- verificacao de bloqueio (RN41), limite free 20 DMs (RN39), reutilizar DM existente
- [ ] `createStaticChatRoom()` -- automatico ao criar static (RN38)
- [ ] `addMemberToStaticChat()`, `removeMemberFromStaticChat()`, `deactivateStaticChat()`
- [ ] `sendMessage()` -- sanitizacao XSS, broadcast STOMP, publicar no Redis Pub/Sub
- [ ] `getMessages()` -- paginacao cursor-based
- [ ] `markAsRead()` -- bulk update, broadcast read receipts
- [ ] `broadcastTyping()` -- apenas broadcast, sem persistencia
- [ ] Consumir eventos: StaticCreatedEvent, MemberJoinedEvent, MemberLeftEvent, StaticDissolvedEvent
- [ ] Testes

---

### CHAT-05: Implementar ChatWebSocketController (STOMP)

**Labels:** `backend`, `chat`, `priority:high`

**Checklist:**

- [ ] `@MessageMapping("/chat.send")` -- enviar mensagem
- [ ] `@MessageMapping("/chat.typing")` -- indicador de digitacao
- [ ] `@MessageMapping("/chat.read")` -- marcar como lido
- [ ] Broadcast para `/topic/chat.{roomId}`
- [ ] Testes

---

### CHAT-06: Implementar Redis Pub/Sub para multi-instance

**Labels:** `backend`, `chat`, `infra`, `priority:medium`

**Checklist:**

- [ ] `ChatRedisConfig` -- RedisMessageListenerContainer
- [ ] `ChatRedisPublisher` -- publica em `ws:chat:{roomId}`
- [ ] `ChatRedisSubscriber` -- recebe e distribui para clients locais
- [ ] Testes

---

### CHAT-07: Implementar PresenceService

**Labels:** `backend`, `chat`, `priority:medium`

**Checklist:**

- [ ] `updatePresence()` -- heartbeat no Redis com TTL 5min
- [ ] `isOnline()`, `getRoomPresence()`, `removePresence()`
- [ ] Hook no connect/disconnect do WebSocket
- [ ] Testes

---

### CHAT-08: Implementar ChatController (REST fallback)

**Labels:** `backend`, `chat`, `priority:high`

**Checklist:**

- [ ] GET `/chats` -- listar conversas
- [ ] POST `/chats/dm` -- iniciar DM
- [ ] GET `/chats/{id}/messages` -- historico
- [ ] POST `/chats/{id}/messages` -- enviar (fallback REST)
- [ ] Testes

---

### CHAT-09: Frontend -- Paginas de chat

**Labels:** `frontend`, `chat`, `priority:high`

**Checklist:**

- [ ] `/chat` -- layout split (lista + conversa)
- [ ] `/chat/[id]` -- conversa ativa com scroll infinito
- [ ] Componentes: ChatRoomList, ChatRoomItem, MessageList, MessageBubble, TypingIndicator, MessageInput
- [ ] Zustand store `chat-store.ts` (activeChatRoomId, typingUsers, onlineUsers)
- [ ] `hooks/use-websocket.ts` -- conexao STOMP com SockJS
- [ ] `hooks/use-chat-websocket.ts` -- subscribe por room, sendMessage, sendTyping, markRead
- [ ] `hooks/use-presence.ts` -- heartbeat de presenca
- [ ] Hooks TanStack Query: useChatRooms, useChatMessages (infinite query), useCreateDm
- [ ] Instalar @stomp/stompjs + sockjs-client

---

## Milestone 8: Notificacoes e Polimento

> **Depende de:** Milestones 1-7
> **Referencia:** `docs/architecture.md` secao 5.2

### NOTIF-01: Criar migration V9 e V10 (notifications, user_blocks)

**Labels:** `database`, `priority:medium`

**Checklist:**

- [ ] `V9__create_notifications.sql`
- [ ] `V10__create_user_blocks.sql`
- [ ] `V11__create_indexes.sql` (indices adicionais globais)

---

### NOTIF-02: Implementar NotificationService

**Labels:** `backend`, `priority:medium`

**Checklist:**

- [ ] Entity `Notification`
- [ ] Consumir todos os eventos de dominio e gerar notificacoes in-app
- [ ] Envio via WebSocket (`/user/queue/notifications`)
- [ ] Envio de email para eventos criticos (verificacao, reset, pagamento)
- [ ] Controller: GET /notifications, PUT /notifications/{id}/read, PUT /notifications/read-all

---

### NOTIF-03: Implementar UserBlockService

**Labels:** `backend`, `priority:medium`

**Checklist:**

- [ ] Block/Unblock usuario
- [ ] Integrar com ChatService (desativar DMs) e MatchingService (excluir de matches)
- [ ] Controller: POST /users/block, DELETE /users/block/{userId}, GET /users/blocked

---

### NOTIF-04: Frontend -- Sistema de notificacoes

**Labels:** `frontend`, `priority:medium`

**Checklist:**

- [ ] Componente NotificationBell no header com badge de contagem
- [ ] Dropdown de notificacoes com lista e marcar como lida
- [ ] Zustand store `notification-store.ts`
- [ ] Hook `use-notifications.ts` (WebSocket subscription + REST queries)
- [ ] Toast notifications via Sonner

---

### NOTIF-05: Frontend -- Dashboard

**Labels:** `frontend`, `priority:medium`

**Checklist:**

- [ ] `/dashboard` -- pagina inicial apos login
- [ ] Resumo de: statics ativas, proximos eventos, candidaturas pendentes, novos matches, notificacoes
- [ ] Widgets: ProximoEventoCard, MatchesPendentesCard, CandidaturasPendentesCard

---

### NOTIF-06: Frontend -- Landing page (SSG)

**Labels:** `frontend`, `priority:low`

**Checklist:**

- [ ] `/[locale]/page.tsx` -- landing page estatica
- [ ] Hero section, Features section, How it works, Pricing, FAQ
- [ ] SEO meta tags
- [ ] SSG (gerado em build time)

---

## Milestone 9: Testes e Qualidade

> **Depende de:** Milestones 1-8

### TEST-01: Testes unitarios backend (Services)

**Labels:** `backend`, `priority:high`

**Checklist:**

- [ ] AuthServiceTest, ProfileServiceTest, CharacterServiceTest
- [ ] StaticServiceTest, VacancyServiceTest, ApplicationServiceTest, InvitationServiceTest
- [ ] MatchCalculationServiceTest, MatchingServiceTest
- [ ] EventServiceTest, AttendanceServiceTest
- [ ] ReviewServiceTest, CoexistenceValidatorTest, ReputationServiceTest
- [ ] ChatServiceTest, MessageServiceTest
- [ ] SubscriptionServiceTest
- [ ] Cobertura >= 80%

---

### TEST-02: Testes de integracao backend (Controllers + DB)

**Labels:** `backend`, `priority:high`

**Checklist:**

- [ ] AbstractIntegrationTest com TestContainers (PostgreSQL + Redis)
- [ ] AuthControllerIntegrationTest (fluxo completo de registro/login/refresh)
- [ ] StaticControllerIntegrationTest (criar static, vagas, candidaturas)
- [ ] StripeWebhookControllerTest (simular webhooks)
- [ ] Spring Security Test (verificar endpoints publicos/protegidos)

---

### TEST-03: Testes frontend (Vitest + React Testing Library)

**Labels:** `frontend`, `priority:high`

**Checklist:**

- [ ] Testes de componentes: LoginForm, RegisterForm, StaticCard, MatchCard, ReviewForm
- [ ] Testes de hooks: useAuth, useWebSocket
- [ ] Testes de stores: auth-store, chat-store
- [ ] Testes de schemas Zod
- [ ] MSW para mock de API
- [ ] Cobertura >= 75%

---

### TEST-04: Testes E2E (Playwright)

**Labels:** `frontend`, `priority:medium`

**Checklist:**

- [ ] Fluxo de autenticacao (register -> verify -> login -> logout)
- [ ] Fluxo de static (criar -> publicar vaga -> candidatar -> aceitar)
- [ ] Fluxo de chat (criar DM -> enviar mensagem)
- [ ] Fluxo de premium (selecionar plano -> checkout -> verificar status)
- [ ] Configurar Playwright com ambiente de teste

---

## Ordem Recomendada de Desenvolvimento

```
Milestone 0: Infraestrutura (INFRA-01 a INFRA-05)
    |
    v
Milestone 1: Auth & Perfil (AUTH-01 a AUTH-18)
    |
    +---> Milestone 2: Premium (PREMIUM-01 a PREMIUM-10)
    |
    v
Milestone 3: Statics (STATIC-01 a STATIC-12)
    |
    +---> Milestone 4: Matching (MATCH-01 a MATCH-06)
    |
    +---> Milestone 5: Calendario (CAL-01 a CAL-07)
    |
    +---> Milestone 6: Reputacao (REP-01 a REP-06)
    |
    +---> Milestone 7: Chat (CHAT-01 a CHAT-09)
    |
    v
Milestone 8: Notificacoes e Polimento (NOTIF-01 a NOTIF-06)
    |
    v
Milestone 9: Testes e Qualidade (TEST-01 a TEST-04)
```

**Total: 10 milestones, 80 issues**
