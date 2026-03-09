1 # FFXIV Static Planner - Tasks de Desenvolvimento
2
3 Documento com todas as issues para criacao manual no GitHub.
4 Organizacao: issues granulares agrupadas por milestone (modulo).
5
6 ---
7
8 ## Labels
9
10 Criar as seguintes labels antes de criar as issues:
11
12 | Label | Cor | Descricao |
13 |-------|-----|-----------|
14 | `backend` | `#0E8A16` | Trabalho no Spring Boot / Java |
15 | `frontend` | `#1D76DB` | Trabalho no Next.js / React |
16 | `database` | `#FBCA04` | Migrations, schema, indices |
17 | `infra` | `#D93F0B` | Docker, CI/CD, deploy, config |
18 | `docs` | `#C5DEF5` | Documentacao |
19 | `auth` | `#B60205` | Modulo de autenticacao |
20 | `profile` | `#E99695` | Modulo de perfil |
21 | `static` | `#0075CA` | Modulo de statics |
22 | `matching` | `#7057FF` | Modulo de matching |
23 | `calendar` | `#008672` | Modulo de calendario |
24 | `reputation` | `#D876E3` | Modulo de reputacao |
25 | `chat` | `#F9D0C4` | Modulo de chat |
26 | `premium` | `#FEF2C0` | Modulo premium/pagamentos |
27 | `priority:high` | `#B60205` | Prioridade alta |
28 | `priority:medium` | `#FBCA04` | Prioridade media |
29 | `priority:low` | `#0E8A16` | Prioridade baixa |
30
31 ---
32
33 ## Milestone 0: Infraestrutura e Setup do Projeto
34
35 ### INFRA-01: Inicializar monorepo com Turborepo
36
37 **Labels:** `infra`, `priority:high`
38
39 **Descricao:**
40 Configurar o monorepo com Turborepo 2.x e npm workspaces.
41
42 **Checklist:**
43 - [ ] Criar `package.json` root com workspaces
44 - [ ] Criar `turbo.json` com pipelines (dev, build, lint, test)
45 - [ ] Criar `apps/web/` (Next.js 15) com `create-next-app`
46 - [ ] Criar `apps/api/` (Spring Boot 3.3) com Spring Initializr
47 - [ ] Criar `packages/eslint-config/`
48 - [ ] Criar `packages/typescript-config/`
49 - [ ] Validar `npm run dev` roda ambos os apps
50 - [ ] Validar `npm run build` builda tudo via Turborepo
51
52 ---
53
54 ### INFRA-02: Configurar Docker Compose (dev)
55
56 **Labels:** `infra`, `priority:high`
57
58 **Descricao:**
59 Criar `docker-compose.yml` para ambiente de desenvolvimento local.
60
61 **Checklist:**
62 - [ ] PostgreSQL 16 Alpine com volume persistente
63 - [ ] Redis 7 Alpine
64 - [ ] Container do backend (Spring Boot) com hot reload
65 - [ ] Container do frontend (Next.js) com hot reload
66 - [ ] `.env.example` com todas as variaveis de ambiente
67 - [ ] `.dockerignore` para ambos os apps
68 - [ ] Documentar comandos no README
69
70 ---
71
72 ### INFRA-03: Configurar Dockerfiles de producao (multi-stage)
73
74 **Labels:** `infra`, `priority:medium`
75
76 **Descricao:**
77 Criar Dockerfiles multi-stage otimizados para producao.
78
79 **Checklist:**
80 - [ ] `apps/api/Dockerfile` (temurin:21-jdk build -> temurin:21-jre runtime)
81 - [ ] `apps/web/Dockerfile` (node:22 build -> standalone output)
82 - [ ] Nginx config para reverse proxy
83 - [ ] docker-compose.prod.yml
84
85 ---
86
87 ### INFRA-04: Configurar GitHub Actions (CI)
88
89 **Labels:** `infra`, `priority:medium`
90
91 **Descricao:**
92 Pipeline CI com lint, testes e build.
93
94 **Checklist:**
95 - [ ] Workflow `pr-validation.yml` (lint, test frontend, test backend, build)
96 - [ ] Workflow `staging-deploy.yml` (build Docker, push registry, deploy)
97 - [ ] Workflow `production-deploy.yml` (E2E, build prod images, deploy)
98 - [ ] Configurar services (PostgreSQL, Redis) nos workflows de teste
99
100 ---
101
102 ### INFRA-05: Configurar ESLint, Prettier e TypeScript compartilhados
103
104 **Labels:** `infra`, `frontend`, `priority:high`
105
106 **Descricao:**
107 Configuracoes compartilhadas via packages do monorepo.
108
109 **Checklist:**
110 - [ ] `packages/eslint-config/` com regras base
111 - [ ] `packages/typescript-config/` com tsconfig base
112 - [ ] `apps/web` estende as configs compartilhadas
113 - [ ] Prettier configurado no root
114 - [ ] Validar `npm run lint` funciona
115
116 ---
117
118 ## Milestone 1: Autenticacao & Perfil (Auth/Profile)
119
120 > **Depende de:** Milestone 0
121 > **Referencia:** `docs/features/01-auth-profile.md`
122
123 ### AUTH-01: Criar migration V1 (users, characters, character_jobs, refresh_tokens)
124
125 **Labels:** `database`, `auth`, `profile`, `priority:high`
126
127 **Descricao:**
128 Criar a migration Flyway `V1__create_users_and_characters.sql`.
129
130 **Checklist:**
131 - [ ] Tabela `users` com todos os campos (email, password_hash, locale, is_premium, oauth, lockout, soft delete)
132 - [ ] Tabela `characters` com campos e FK para users
133 - [ ] Tabela `character_jobs` com FK para characters e constraint unique (character_id, job_name)
134 - [ ] Tabela `refresh_tokens` com FK para users
135 - [ ] Todos os indices definidos na feature doc
136 - [ ] Validar migration roda sem erros no PostgreSQL 16
137
138 ---
139
140 ### AUTH-02: Criar entities JPA (User, Character, CharacterJob, RefreshToken, AuditableEntity)
141
142 **Labels:** `backend`, `auth`, `profile`, `priority:high`
143
144 **Descricao:**
145 Implementar as entidades JPA com anotacoes, relacionamentos e enum Role.
146
147 **Checklist:**
148 - [ ] `AuditableEntity` (@MappedSuperclass com createdAt/updatedAt)
149 - [ ] `User` entity com todos os campos e relacionamentos
150 - [ ] `Character` entity com FK para User e lista de jobs
151 - [ ] `CharacterJob` entity com FK para Character e enum Role
152 - [ ] `RefreshToken` entity com FK para User
153 - [ ] Enum `Role` (TANK, HEALER, DPS)
154 - [ ] Configurar JPA Auditing (`@EnableJpaAuditing`)
155
156 ---
157
158 ### AUTH-03: Criar repositories (UserRepository, CharacterRepository, etc.)
159
160 **Labels:** `backend`, `auth`, `profile`, `priority:high`
161
162 **Descricao:**
163 Implementar interfaces Spring Data JPA com queries customizadas.
164
165 **Checklist:**
166 - [ ] `UserRepository` (findByEmail, findByOAuthProvider, findActiveById)
167 - [ ] `CharacterRepository` (findByUserId, findByLodestoneId, findMainByUserId, findLFSByDataCenter)
168 - [ ] `CharacterJobRepository` (findByCharacterId, findMainByCharacterId)
169 - [ ] `RefreshTokenRepository` (findByTokenHash, revokeAllByUserId, deleteExpired)
170
171 ---
172
173 ### AUTH-04: Implementar JWT (JwtTokenProvider, JwtAuthenticationFilter, JwtProperties)
174
175 **Labels:** `backend`, `auth`, `priority:high`
176
177 **Descricao:**
178 Implementar infraestrutura JWT completa.
179
180 **Checklist:**
181 - [ ] `JwtProperties` (@ConfigurationProperties com secret, accessExpiration, refreshExpiration)
182 - [ ] `JwtTokenProvider` (generateAccessToken, generateRefreshToken, getUserIdFromToken, validateToken)
183 - [ ] `JwtAuthenticationFilter` (OncePerRequestFilter - extrai token do header, valida, seta SecurityContext)
184 - [ ] Configurar jjwt-api/impl/jackson no build.gradle.kts
185 - [ ] Testes unitarios do JwtTokenProvider
186
187 ---
188
189 ### AUTH-05: Configurar Spring Security (SecurityConfig)
190
191 **Labels:** `backend`, `auth`, `priority:high`
192
193 **Descricao:**
194 Configuracao central de seguranca com JWT.
195
196 **Checklist:**
197 - [ ] `SecurityConfig` com HttpSecurity configurado
198 - [ ] Endpoints publicos: register, login, refresh, forgot-password, reset-password, perfis publicos
199 - [ ] Endpoints autenticados: todos os demais
200 - [ ] CSRF desabilitado (API stateless)
201 - [ ] CORS configurado (`CorsConfig`)
202 - [ ] Registrar JwtAuthenticationFilter antes do UsernamePasswordAuthenticationFilter
203 - [ ] BCrypt PasswordEncoder bean
204
205 ---
206
207 ### AUTH-06: Implementar AuthService (register, login, refresh, logout, forgot/reset password)
208
209 **Labels:** `backend`, `auth`, `priority:high`
210
211 **Descricao:**
212 Servico de autenticacao completo.
213
214 **Checklist:**
215 - [ ] `register()` -- validacao de email unico, hash BCrypt, evento UserRegisteredEvent
216 - [ ] `login()` -- verificacao de lockout (5 tentativas = 15min), validacao de senha, geracao de tokens
217 - [ ] `refresh()` -- refresh token rotation, geracao de novo par de tokens
218 - [ ] `logout()` -- revogacao de refresh token, limpeza de cookie
219 - [ ] `forgotPassword()` -- geracao de token de reset, armazenamento no Redis com TTL 1h
220 - [ ] `resetPassword()` -- validacao de token, atualizacao de senha, revogacao de todos os refresh tokens
221 - [ ] Testes unitarios com Mockito
222
223 ---
224
225 ### AUTH-07: Implementar AuthController
226
227 **Labels:** `backend`, `auth`, `priority:high`
228
229 **Descricao:**
230 Controller REST para endpoints de autenticacao.
231
232 **Checklist:**
233 - [ ] POST `/api/v1/auth/register` -> 201
234 - [ ] POST `/api/v1/auth/login` -> 200 (seta cookie httpOnly com refresh token)
235 - [ ] POST `/api/v1/auth/refresh` -> 200 (le cookie, rotaciona token)
236 - [ ] POST `/api/v1/auth/logout` -> 204
237 - [ ] POST `/api/v1/auth/forgot-password` -> 200
238 - [ ] POST `/api/v1/auth/reset-password` -> 200
239 - [ ] Request/Response DTOs (RegisterRequest, LoginRequest, LoginResponse, etc.)
240 - [ ] Bean Validation nos DTOs (@NotBlank, @Email, @Size, @Pattern)
241 - [ ] Testes de integracao com MockMvc
242
243 ---
244
245 ### AUTH-08: Implementar OAuth 2.0 (Google + Discord)
246
247 **Labels:** `backend`, `auth`, `priority:medium`
248
249 **Descricao:**
250 Autenticacao via provedores OAuth 2.0.
251
252 **Checklist:**
253 - [ ] `CustomOAuth2UserService` (cria/vincula usuario)
254 - [ ] `OAuth2SuccessHandler` (gera JWT e redireciona para frontend)
255 - [ ] Configuracao no application.yml (client-id, client-secret, redirect-uri)
256 - [ ] Suporte a Google e Discord como provedores
257 - [ ] Testes
258
259 ---
260
261 ### AUTH-09: Implementar ProfileService e ProfileController
262
263 **Labels:** `backend`, `profile`, `priority:high`
264
265 **Descricao:**
266 CRUD de perfil do usuario.
267
268 **Checklist:**
269 - [ ] `ProfileService.getMyProfile()` -- perfil completo com characters e jobs
270 - [ ] `ProfileService.getPublicProfile()` -- perfil publico com cache Redis (5min)
271 - [ ] `ProfileService.updateProfile()` -- atualiza locale, bio, availability; invalida cache
272 - [ ] `ProfileController` com endpoints GET/PUT `/profile/me` e GET `/users/{id}/profile`
273 - [ ] MapStruct mappers (UserMapper, CharacterMapper)
274 - [ ] Evento ProfileUpdatedEvent
275 - [ ] Testes
276
277 ---
278
279 ### AUTH-10: Implementar CharacterService e CharacterController
280
281 **Labels:** `backend`, `profile`, `priority:high`
282
283 **Descricao:**
284 CRUD de personagens com logica de main character.
285
286 **Checklist:**
287 - [ ] `createCharacter()` -- logica de isMain (primeiro = main automatico)
288 - [ ] `updateCharacter()` -- verificacao de ownership
289 - [ ] `deleteCharacter()` -- nao pode ser o unico, promove outro a main se necessario
290 - [ ] CharacterController com endpoints POST/PUT/DELETE `/characters`
291 - [ ] CharacterJobService (addJob, updateJob, deleteJob) com endpoints
292 - [ ] MapStruct mappers (CharacterMapper, JobMapper)
293 - [ ] Testes
294
295 ---
296
297 ### AUTH-11: Implementar verificacao Lodestone (LodestoneClient + fluxo de verificacao)
298
299 **Labels:** `backend`, `profile`, `priority:medium`
300
301 **Descricao:**
302 Integracao com XIVAPI para verificacao de personagens.
303
304 **Checklist:**
305 - [ ] `LodestoneClient` (WebFlux) -- buscar perfil por nome+server, buscar por lodestoneId
306 - [ ] `LodestoneMapper` -- mapear response da API para dados do character
307 - [ ] `CharacterService.startVerification()` -- gera codigo, armazena no Redis, retorna instrucoes
308 - [ ] `CharacterService.confirmVerification()` -- verifica codigo na bio, importa dados, marca verificado
309 - [ ] Endpoints POST `/characters/{id}/verify` e POST `/characters/{id}/verify/confirm`
310 - [ ] Cache Redis: `lodestone:{lodestoneId}` TTL 1h
311 - [ ] Testes (mock da API externa)
312
313 ---
314
315 ### AUTH-12: Configurar Redis (cache, sessoes, rate limiting)
316
317 **Labels:** `backend`, `infra`, `auth`, `priority:high`
318
319 **Descricao:**
320 Configuracao do Redis para cache, sessoes JWT e rate limiting.
321
322 **Checklist:**
323 - [ ] `RedisConfig` (RedisTemplate, CacheManager)
324 - [ ] Configuracao de cache com TTLs por tipo (profiles 5min, lodestone 1h)
325 - [ ] Rate limiting para login (sliding window counter)
326 - [ ] Armazenamento de refresh token hash em `session:{userId}`
327 - [ ] Armazenamento de tokens de verificacao e reset
328
329 ---
330
331 ### AUTH-13: Implementar GlobalExceptionHandler (RFC 7807)
332
333 **Labels:** `backend`, `priority:high`
334
335 **Descricao:**
336 Handler global de excecoes com formato RFC 7807 Problem Details.
337
338 **Checklist:**
339 - [ ] `GlobalExceptionHandler` (@RestControllerAdvice)
340 - [ ] `BusinessException` (base para excecoes de negocio)
341 - [ ] `ResourceNotFoundException` (404)
342 - [ ] `ApiError` DTO (RFC 7807 format)
343 - [ ] Handlers para: ValidationException (400), AuthenticationException (401), AccessDeniedException (403), ResourceNotFoundException (404), BusinessException (409/422), RateLimitException (429)
344 - [ ] Testes
345
346 ---
347
348 ### AUTH-14: Frontend -- Paginas de autenticacao (Login, Register, Forgot Password)
349
350 **Labels:** `frontend`, `auth`, `priority:high`
351
352 **Descricao:**
353 Implementar paginas de autenticacao com formularios validados.
354
355 **Checklist:**
356 - [ ] `/[locale]/(auth)/login/page.tsx` -- formulario de login + botoes OAuth
357 - [ ] `/[locale]/(auth)/register/page.tsx` -- formulario de registro
358 - [ ] `/[locale]/(auth)/forgot-password/page.tsx` -- solicitar reset
359 - [ ] `/[locale]/(auth)/reset-password/page.tsx` -- redefinir senha (com token na URL)
360 - [ ] Schemas Zod: `loginSchema`, `registerSchema`, `forgotPasswordSchema`, `resetPasswordSchema`
361 - [ ] React Hook Form integrado com Zod
362 - [ ] Componentes ShadCN/UI (Input, Button, Card, Form)
363
364 ---
365
366 ### AUTH-15: Frontend -- Zustand auth store e Axios interceptors
367
368 **Labels:** `frontend`, `auth`, `priority:high`
369
370 **Descricao:**
371 Gerenciamento de estado de autenticacao e cliente HTTP.
372
373 **Checklist:**
374 - [ ] `stores/auth-store.ts` (Zustand -- accessToken, user, isAuthenticated, setAuth, clearAuth)
375 - [ ] `lib/api.ts` (Axios instance com base URL)
376 - [ ] Request interceptor: injeta Authorization Bearer header
377 - [ ] Response interceptor: refresh automatico em 401, redirect para login se refresh falha
378 - [ ] `hooks/use-auth.ts` (useLogin, useRegister, useLogout, useRefreshToken)
379
380 ---
381
382 ### AUTH-16: Frontend -- Paginas de perfil e gerenciamento de personagens
383
384 **Labels:** `frontend`, `profile`, `priority:high`
385
386 **Descricao:**
387 Paginas de perfil, edicao e gerenciamento de characters/jobs.
388
389 **Checklist:**
390 - [ ] `/[locale]/(main)/profile/page.tsx` -- meu perfil
391 - [ ] `/[locale]/(main)/profile/edit/page.tsx` -- editar perfil
392 - [ ] `/[locale]/(main)/profile/characters/page.tsx` -- CRUD de characters e jobs
393 - [ ] Componentes: ProfileHeader, CharacterCard, JobBadge, CharacterForm, JobForm
394 - [ ] Fluxo de verificacao Lodestone (VerificationCodeDisplay, ConfirmButton)
395 - [ ] `hooks/use-profile.ts`, `hooks/use-characters.ts`
396 - [ ] Schemas Zod: `profileSchema`, `characterSchema`, `jobSchema`
397
398 ---
399
400 ### AUTH-17: Frontend -- Configurar i18n (next-intl)
401
402 **Labels:** `frontend`, `infra`, `priority:high`
403
404 **Descricao:**
405 Setup de internacionalizacao com next-intl.
406
407 **Checklist:**
408 - [ ] Instalar e configurar `next-intl` com App Router
409 - [ ] Middleware de deteccao de locale e redirect
410 - [ ] Segmento `[locale]` no App Router
411 - [ ] Arquivos de traducao iniciais: `messages/en.json`, `messages/pt.json`
412 - [ ] Namespaces: `auth.*`, `profile.*`, `character.*`, `validation.*`, `ffxiv.*`
413 - [ ] Componente `LocaleSwitcher` no header
414
415 ---
416
417 ### AUTH-18: Frontend -- Layout base (Header, Sidebar, Footer, middleware de auth)
418
419 **Labels:** `frontend`, `infra`, `priority:high`
420
421 **Descricao:**
422 Layout compartilhado e middleware de protecao de rotas.
423
424 **Checklist:**
425 - [ ] `app/[locale]/layout.tsx` -- root layout com providers (Zustand, TanStack Query, next-intl)
426 - [ ] `app/[locale]/(auth)/layout.tsx` -- layout de autenticacao (centralizado, sem sidebar)
427 - [ ] `app/[locale]/(main)/layout.tsx` -- layout autenticado (com header e sidebar)
428 - [ ] Componentes: Header (logo, nav, user menu, locale switcher), Sidebar (nav links), Footer
429 - [ ] `middleware.ts` -- redirect para login se nao autenticado em rotas protegidas
430 - [ ] Configurar Tailwind CSS 4 e ShadCN/UI
431 - [ ] Dark mode (via Tailwind + next-themes)
432
433 ---
434
435 ## Milestone 2: Premium e Assinaturas
436
437 > **Depende de:** Milestone 1
438 > **Referencia:** `docs/features/07-premium.md`
439
440 ### PREMIUM-01: Criar migration V8 (subscriptions, payment_history)
441
442 **Labels:** `database`, `premium`, `priority:high`
443
444 **Descricao:**
445 Migration Flyway para tabelas de assinaturas e pagamentos.
446
447 **Checklist:**
448 - [ ] Tabela `subscriptions` com todos os campos e FK para users
449 - [ ] Tabela `payment_history` com FK para subscriptions
450 - [ ] Indices conforme feature doc
451 - [ ] Validar migration
452
453 ---
454
455 ### PREMIUM-02: Criar entities JPA (Subscription, PaymentHistory) e enums
456
457 **Labels:** `backend`, `premium`, `priority:high`
458
459 **Checklist:**
460 - [ ] `Subscription` entity
461 - [ ] `PaymentHistory` entity
462 - [ ] Enums: `SubscriptionPlan`, `SubscriptionStatus`, `PaymentStatus`
463 - [ ] Repositories com queries customizadas
464
465 ---
466
467 ### PREMIUM-03: Implementar StripeService (integracao Stripe)
468
469 **Labels:** `backend`, `premium`, `priority:high`
470
471 **Checklist:**
472 - [ ] Adicionar `stripe-java` ao build.gradle.kts
473 - [ ] `StripeService` (getOrCreateCustomer, createCheckoutSession, cancelSubscription, reactivateSubscription, constructWebhookEvent)
474 - [ ] Configuracao de chaves Stripe no application.yml
475 - [ ] Testes com mocks do Stripe
476
477 ---
478
479 ### PREMIUM-04: Implementar SubscriptionService
480
481 **Labels:** `backend`, `premium`, `priority:high`
482
483 **Checklist:**
484 - [ ] `getAvailablePlans()` -- lista planos com precos
485 - [ ] `createSubscription()` -- cria checkout session no Stripe
486 - [ ] `getMySubscription()` -- retorna assinatura atual
487 - [ ] `cancelSubscription()` -- cancel_at_period_end (RN43)
488 - [ ] `reactivateSubscription()` -- remove cancelamento
489 - [ ] `activateTrial()` -- trial 7 dias, unico por conta (RN45)
490 - [ ] `expireSubscription()` -- downgrade (RN44)
491 - [ ] Eventos: SubscriptionActivatedEvent, SubscriptionExpiredEvent, etc.
492 - [ ] Testes
493
494 ---
495
496 ### PREMIUM-05: Implementar StripeWebhookController
497
498 **Labels:** `backend`, `premium`, `priority:high`
499
500 **Checklist:**
501 - [ ] POST `/api/v1/webhooks/stripe` -- recebe e verifica webhooks
502 - [ ] Handler `checkout.session.completed` -- cria Subscription, ativa premium
503 - [ ] Handler `invoice.paid` -- renova periodo, registra pagamento
504 - [ ] Handler `invoice.payment_failed` -- marca PAST_DUE, notifica usuario
505 - [ ] Handler `customer.subscription.deleted` -- expira assinatura
506 - [ ] Idempotencia via stripe_payment_id unique
507 - [ ] Testes
508
509 ---
510
511 ### PREMIUM-06: Implementar @RequiresPremium (Aspect)
512
513 **Labels:** `backend`, `premium`, `priority:high`
514
515 **Checklist:**
516 - [ ] Annotation `@RequiresPremium`
517 - [ ] `PremiumAspect` (@Around) -- verifica isPremium, lanca PremiumRequiredException se false
518 - [ ] Testes
519
520 ---
521
522 ### PREMIUM-07: Implementar SubscriptionController e DTOs
523
524 **Labels:** `backend`, `premium`, `priority:high`
525
526 **Checklist:**
527 - [ ] DTOs: CreateSubscriptionRequest, PlanResponse, SubscriptionResponse, CheckoutSessionResponse, PaymentHistoryResponse
528 - [ ] SubscriptionMapper (MapStruct)
529 - [ ] SubscriptionController com todos os endpoints
530 - [ ] Testes de integracao
531
532 ---
533
534 ### PREMIUM-08: Implementar SubscriptionScheduler (job diario)
535
536 **Labels:** `backend`, `premium`, `priority:medium`
537
538 **Checklist:**
539 - [ ] Quartz job diario as 01:00 UTC
540 - [ ] Expirar assinaturas ACTIVE/TRIALING vencidas
541 - [ ] Expirar PAST_DUE com mais de 7 dias
542 - [ ] Alertar assinaturas expirando em 3 dias
543 - [ ] Atualizar CANCELED com endDate passado para EXPIRED
544
545 ---
546
547 ### PREMIUM-09: Frontend -- Pagina de planos premium
548
549 **Labels:** `frontend`, `premium`, `priority:high`
550
551 **Checklist:**
552 - [ ] `/[locale]/(main)/premium/page.tsx`
553 - [ ] Componentes: PlanComparison, PlanCard, PriceDisplay, FeatureList, SubscribeButton
554 - [ ] Hook `usePlans()`, `useCreateSubscription()`
555 - [ ] Redirect para Stripe Checkout
556
557 ---
558
559 ### PREMIUM-10: Frontend -- Pagina de gerenciamento de assinatura
560
561 **Labels:** `frontend`, `premium`, `priority:high`
562
563 **Checklist:**
564 - [ ] `/[locale]/(main)/account/subscription/page.tsx`
565 - [ ] Componentes: SubscriptionStatus, CancelButton, ReactivateButton, PaymentHistoryTable
566 - [ ] `/[locale]/(main)/account/settings/page.tsx`
567 - [ ] Hooks: `useMySubscription()`, `useCancelSubscription()`, `usePaymentHistory()`
568 - [ ] Trial banner com `useActivateTrial()`
569
570 ---
571
572 ## Milestone 3: Gestao de Statics
573
574 > **Depende de:** Milestone 1, Milestone 2
575 > **Referencia:** `docs/features/02-static-management.md`
576
577 ### STATIC-01: Criar migrations V2 e V3 (statics, memberships, vacancies, applications, invitations)
578
579 **Labels:** `database`, `static`, `priority:high`
580
581 **Checklist:**
582 - [ ] `V2__create_statics_and_memberships.sql` (statics, static_memberships)
583 - [ ] `V3__create_vacancies_applications_invitations.sql` (static_vacancies, applications, invitations)
584 - [ ] Extensao `pg_trgm` para busca textual por nome de static
585 - [ ] Todos os indices (incluindo GIN trigram)
586 - [ ] Validar migrations
587
588 ---
589
590 ### STATIC-02: Criar entities JPA (Static, StaticMembership, StaticVacancy, Application, Invitation)
591
592 **Labels:** `backend`, `static`, `priority:high`
593
594 **Checklist:**
595 - [ ] `Static` entity com enums (StaticStatus)
596 - [ ] `StaticMembership` entity
597 - [ ] `StaticVacancy` entity com JSONB fields
598 - [ ] `Application` entity com enum ApplicationStatus
599 - [ ] `Invitation` entity com enum InvitationStatus
600 - [ ] Todos os relacionamentos JPA
601
602 ---
603
604 ### STATIC-03: Criar repositories com queries e Specifications
605
606 **Labels:** `backend`, `static`, `priority:high`
607
608 **Checklist:**
609 - [ ] `StaticRepository` com findActiveById, findByMemberUserId, countByLeaderUserId
610 - [ ] `StaticSpecifications` para busca dinamica (filtros por DC, role, language, status, nome)
611 - [ ] `StaticMembershipRepository` (findActiveByStaticId, findLeaderByStaticId, countActive)
612 - [ ] `StaticVacancyRepository` (findByStaticIdAndIsOpenTrue)
613 - [ ] `ApplicationRepository` (findPendingByStaticId, findByUserId, findExpired)
614 - [ ] `InvitationRepository` (findPendingByUserId, findExpired)
615
616 ---
617
618 ### STATIC-04: Implementar StaticService (CRUD, gestao de membros)
619
620 **Labels:** `backend`, `static`, `priority:high`
621
622 **Checklist:**
623 - [ ] `createStatic()` -- verificacao de limite free/premium (RN10), cria membership de lider
624 - [ ] `updateStatic()` -- verificacao de lideranca
625 - [ ] `dissolveStatic()` -- soft delete, notificacao de membros (RN12)
626 - [ ] `getMembers()`, `updateMember()`, `removeMember()`
627 - [ ] `transferLeadership()` (RN07)
628 - [ ] `leaveStatic()` -- lider deve transferir antes
629 - [ ] Eventos: StaticCreatedEvent, StaticDissolvedEvent, MemberJoinedEvent, MemberLeftEvent
630 - [ ] Testes
631
632 ---
633
634 ### STATIC-05: Implementar VacancyService
635
636 **Labels:** `backend`, `static`, `priority:high`
637
638 **Checklist:**
639 - [ ] `createVacancy()` -- verificacao de lideranca, espaco na static
640 - [ ] `updateVacancy()`, `closeVacancy()`
641 - [ ] `getVacancies()`
642 - [ ] Evento VacancyCreatedEvent
643 - [ ] Testes
644
645 ---
646
647 ### STATIC-06: Implementar ApplicationService
648
649 **Labels:** `backend`, `static`, `priority:high`
650
651 **Checklist:**
652 - [ ] `apply()` -- vaga aberta, character verificado, sem duplicata, nao membro (RN14)
653 - [ ] `acceptApplication()` -- cria membership, fecha vaga, rejeita demais candidaturas
654 - [ ] `rejectApplication()`, `withdrawApplication()`
655 - [ ] `getStaticApplications()`, `getMyApplications()`
656 - [ ] Eventos: ApplicationCreatedEvent, ApplicationAcceptedEvent
657 - [ ] Testes
658
659 ---
660
661 ### STATIC-07: Implementar InvitationService
662
663 **Labels:** `backend`, `static`, `priority:high`
664
665 **Checklist:**
666 - [ ] `invite()` -- verificacao de lideranca, character verificado, sem duplicata (RN15)
667 - [ ] `acceptInvitation()` -- cria membership, fecha vaga se aplicavel
668 - [ ] `rejectInvitation()`, `cancelInvitation()`
669 - [ ] `getMyInvitations()`
670 - [ ] Eventos: InvitationCreatedEvent, InvitationAcceptedEvent
671 - [ ] Testes
672
673 ---
674
675 ### STATIC-08: Implementar SearchService (busca de statics e jogadores)
676
677 **Labels:** `backend`, `static`, `priority:medium`
678
679 **Descricao:**
680 Busca com filtros dinamicos via Specifications.
681
682 **Checklist:**
683 - [ ] `searchStatics()` -- filtros por DC, server, role, language, contentScope, status, query
684 - [ ] `searchPlayers()` -- filtros por DC, role, job, parseTier, lookingForStatic
685 - [ ] SearchController com GET `/search/statics` e `/search/players`
686 - [ ] Paginacao e ordenacao
687 - [ ] Testes
688
689 ---
690
691 ### STATIC-09: Implementar Controllers (Static, Vacancy, Application, Invitation, Search)
692
693 **Labels:** `backend`, `static`, `priority:high`
694
695 **Checklist:**
696 - [ ] `StaticController` -- CRUD, membros, transferencia, leave
697 - [ ] `VacancyController` -- CRUD de vagas
698 - [ ] `ApplicationController` -- aplicar, aceitar, rejeitar, cancelar, listar
699 - [ ] `InvitationController` -- convidar, aceitar, rejeitar, cancelar, listar
700 - [ ] `SearchController` -- buscar statics e jogadores
701 - [ ] Todos os DTOs e MapStruct mappers
702 - [ ] Testes de integracao
703
704 ---
705
706 ### STATIC-10: Implementar ExpirationJob (expirar candidaturas e convites)
707
708 **Labels:** `backend`, `static`, `priority:medium`
709
710 **Checklist:**
711 - [ ] Quartz job diario as 00:00 UTC
712 - [ ] Expirar candidaturas com mais de 14 dias (RN17)
713 - [ ] Expirar convites com mais de 7 dias (RN18)
714 - [ ] Notificar envolvidos
715 - [ ] Testes
716
717 ---
718
719 ### STATIC-11: Frontend -- Paginas de statics (listar, criar, ver, gerenciar)
720
721 **Labels:** `frontend`, `static`, `priority:high`
722
723 **Checklist:**
724 - [ ] `/statics` -- listar minhas statics
725 - [ ] `/statics/create` -- formulario de criacao com Zod
726 - [ ] `/statics/[id]` -- pagina publica da static (SSR + ISR)
727 - [ ] `/statics/[id]/manage` -- painel de gestao (membros, configuracoes, dissolver)
728 - [ ] `/statics/[id]/applications` -- listar e gerenciar candidaturas
729 - [ ] Componentes: StaticCard, MemberList, VacancyForm, ApplicationCard, InvitationDialog
730 - [ ] Hooks: useMyStatics, useStatic, useCreateStatic, useDissolveStatic
731
732 ---
733
734 ### STATIC-12: Frontend -- Paginas de busca (statics e jogadores)
735
736 **Labels:** `frontend`, `static`, `priority:high`
737
738 **Checklist:**
739 - [ ] `/search/statics` -- busca com filtros e paginacao (URL state com nuqs)
740 - [ ] `/search/players` -- busca com filtros e paginacao
741 - [ ] Componentes: SearchFilters, StaticResultList, PlayerResultList, Pagination
742 - [ ] Hooks: useSearchStatics, useSearchPlayers
743 - [ ] Schemas Zod para filtros
744
745 ---
746
747 ## Milestone 4: Matching
748
749 > **Depende de:** Milestone 1, Milestone 3
750 > **Referencia:** `docs/features/03-matching.md`
751
752 ### MATCH-01: Criar migration V4 (match_suggestions, match_preferences)
753
754 **Labels:** `database`, `matching`, `priority:high`
755
756 **Checklist:**
757 - [ ] Tabelas com campos JSONB para criteria e weights
758 - [ ] Indices para busca por character, status e score
759 - [ ] Validar migration
760
761 ---
762
763 ### MATCH-02: Criar entities e repositories
764
765 **Labels:** `backend`, `matching`, `priority:high`
766
767 **Checklist:**
768 - [ ] `MatchSuggestion` entity com enums (MatchTargetType, MatchStatus)
769 - [ ] `MatchPreference` entity com campos JSONB
770 - [ ] Repositories com queries customizadas
771
772 ---
773
774 ### MATCH-03: Implementar MatchCalculationService (algoritmo de scoring)
775
776 **Labels:** `backend`, `matching`, `priority:high`
777
778 **Descricao:**
779 Implementar o algoritmo de matching com 7 criterios ponderados.
780
781 **Checklist:**
782 - [ ] Criterio 1: Role/Job (25%) -- match exato, match de role, sem match
783 - [ ] Criterio 2: Schedule (25%) -- calculo de sobreposicao horaria
784 - [ ] Criterio 3: Data Center (15%) -- mesmo server, mesmo DC, mesma regiao (RN23)
785 - [ ] Criterio 4: Language (10%) -- match de idioma
786 - [ ] Criterio 5: Progression (10%) -- nivel de progressao
787 - [ ] Criterio 6: Content Scope (10%) -- tipos de conteudo
788 - [ ] Criterio 7: Playstyle (5%) -- seriedade e parse tier
789 - [ ] Score final como soma ponderada, threshold 40% (RN22)
790 - [ ] Testes extensivos do algoritmo com cenarios variados
791
792 ---
793
794 ### MATCH-04: Implementar MatchingService e MatchScheduler
795
796 **Labels:** `backend`, `matching`, `priority:high`
797
798 **Checklist:**
799 - [ ] `getMatches()` -- limite free 5/semana (RN20), threshold 40% (RN22)
800 - [ ] `getMatch()`, `dismissMatch()`
801 - [ ] `getPreferences()`, `updatePreferences()` -- pesos devem somar 100 (RN21)
802 - [ ] `MatchScheduler` -- job diario 03:00 UTC com lock distribuido Redis
803 - [ ] Consumir eventos: CharacterVerifiedEvent, ProfileUpdatedEvent, VacancyCreatedEvent
804 - [ ] Cache Redis: `match:{characterId}` 24h, `count:matches:{userId}:{week}` 7d
805 - [ ] Testes
806
807 ---
808
809 ### MATCH-05: Implementar MatchController e DTOs
810
811 **Labels:** `backend`, `matching`, `priority:high`
812
813 **Checklist:**
814 - [ ] Controller com endpoints GET/PUT /matches, GET/PUT /matches/preferences
815 - [ ] DTOs: MatchSuggestionResponse, MatchCriteriaBreakdown, MatchPreferencesResponse
816 - [ ] MatchMapper (MapStruct)
817 - [ ] Testes
818
819 ---
820
821 ### MATCH-06: Frontend -- Paginas de matching
822
823 **Labels:** `frontend`, `matching`, `priority:high`
824
825 **Checklist:**
826 - [ ] `/matches` -- lista de sugestoes com score e preview
827 - [ ] `/matches/preferences` -- editor de pesos (sliders somam 100), filtros, blacklist
828 - [ ] Componentes: MatchCard, CompatibilityScore, CompatibilityBreakdown (grafico radar), PreferencesForm
829 - [ ] Hooks: useMatches, useMatch, useDismissMatch, useMatchPreferences
830 - [ ] Schema Zod: matchPreferencesSchema (refine: pesos somam 100)
831 - [ ] WebSocket subscription para `/user/queue/matches`
832
833 ---
834
835 ## Milestone 5: Calendario
836
837 > **Depende de:** Milestone 3
838 > **Referencia:** `docs/features/04-calendar.md`
839
840 ### CAL-01: Criar migration V5 (raid_events, attendances)
841
842 **Labels:** `database`, `calendar`, `priority:high`
843
844 **Checklist:**
845 - [ ] Tabelas com suporte a eventos recorrentes (recurrence_rule) e PUG (is_public)
846 - [ ] Indices para busca por static, datas e PUGs publicos
847 - [ ] Validar migration
848
849 ---
850
851 ### CAL-02: Criar entities, repositories e DTOs
852
853 **Labels:** `backend`, `calendar`, `priority:high`
854
855 **Checklist:**
856 - [ ] `RaidEvent` entity com enum AttendanceStatus
857 - [ ] `Attendance` entity com constraint unique (event_id, character_id)
858 - [ ] Repositories com queries para static events, public events, user calendar, reminders
859 - [ ] DTOs: CreateEventRequest, EventResponse, AttendanceResponse, CalendarResponse
860 - [ ] EventMapper (MapStruct)
861
862 ---
863
864 ### CAL-03: Implementar EventService e AttendanceService
865
866 **Labels:** `backend`, `calendar`, `priority:high`
867
868 **Checklist:**
869 - [ ] `createStaticEvent()` -- verificacao de lideranca, cria attendances TENTATIVE (RN24)
870 - [ ] `createPugEvent()` -- @RequiresPremium, maxParticipants e dataCenter obrigatorios (RN26)
871 - [ ] `updateEvent()`, `cancelEvent()`
872 - [ ] `getStaticEvents()`, `getPublicEvents()`, `getMyCalendar()`
873 - [ ] `updateAttendance()` -- notifica lider se ABSENT (RN25, RN29)
874 - [ ] `registerForPug()` -- vagas limitadas, waitlist (RN27)
875 - [ ] `unregisterFromPug()` -- promove waitlisted
876 - [ ] Consumir eventos: StaticDissolvedEvent, MemberJoinedEvent, MemberLeftEvent
877 - [ ] WebSocket broadcast via `/topic/static.{staticId}`
878 - [ ] Testes
879
880 ---
881
882 ### CAL-04: Implementar iCalService e exportacao
883
884 **Labels:** `backend`, `calendar`, `priority:medium`
885
886 **Checklist:**
887 - [ ] `ICalService.generateIcal()` -- formato RFC 5545 (VCALENDAR, VEVENT, RRULE, VALARM)
888 - [ ] GET `/calendar/me/ical` -- @RequiresPremium, retorna text/calendar
889 - [ ] Testes
890
891 ---
892
893 ### CAL-05: Implementar ReminderScheduler
894
895 **Labels:** `backend`, `calendar`, `priority:medium`
896
897 **Checklist:**
898 - [ ] Job a cada 15 minutos
899 - [ ] Lembrete 24h antes e 1h antes (RN28)
900 - [ ] Evitar envio duplicado (verificar notificacoes ja enviadas)
901 - [ ] Premium: lembretes customizaveis
902 - [ ] Testes
903
904 ---
905
906 ### CAL-06: Implementar EventController
907
908 **Labels:** `backend`, `calendar`, `priority:high`
909
910 **Checklist:**
911 - [ ] Todos os endpoints REST conforme feature doc
912 - [ ] Testes de integracao
913
914 ---
915
916 ### CAL-07: Frontend -- Paginas de calendario
917
918 **Labels:** `frontend`, `calendar`, `priority:high`
919
920 **Checklist:**
921 - [ ] `/calendar` -- FullCalendar wrapper com modos mes/semana/dia
922 - [ ] `/calendar/events/[id]` -- detalhe do evento com painel de presencas
923 - [ ] `/calendar/events/create` -- formulario com DateTimePicker, RecurrenceEditor, PugOptions
924 - [ ] `/calendar/public` -- lista de eventos PUG por Data Center
925 - [ ] Componentes: CalendarView, EventForm, AttendancePanel, PugEventCard
926 - [ ] Hooks: useMyCalendar, useStaticEvents, usePublicEvents, useCreateEvent, useUpdateAttendance
927 - [ ] WebSocket subscription para `/topic/static.{staticId}` (event updates)
928 - [ ] Instalar e configurar FullCalendar (React wrapper)
929
930 ---
931
932 ## Milestone 6: Reputacao
933
934 > **Depende de:** Milestone 1, Milestone 3
935 > **Referencia:** `docs/features/05-reputation.md`
936
937 ### REP-01: Criar migration V6 (reviews, review_reports)
938
939 **Labels:** `database`, `reputation`, `priority:high`
940
941 **Checklist:**
942 - [ ] Tabelas com CHECK constraints (scores 1-5) e UNIQUEs
943 - [ ] Indices para busca por target, reviewer e reports pendentes
944 - [ ] Validar migration
945
946 ---
947
948 ### REP-02: Criar entities, repositories e DTOs
949
950 **Labels:** `backend`, `reputation`, `priority:high`
951
952 **Checklist:**
953 - [ ] `Review` entity com enums (ReviewTargetType) e soft delete
954 - [ ] `ReviewReport` entity com enums (ReportReason, ReportStatus)
955 - [ ] Repositories com queries para calcular media, contar reviews, buscar por target
956 - [ ] DTOs e ReviewMapper
957
958 ---
959
960 ### REP-03: Implementar ReviewService e CoexistenceValidator
961
962 **Labels:** `backend`, `reputation`, `priority:high`
963
964 **Checklist:**
965 - [ ] `CoexistenceValidator` -- valida 2 semanas de convivencia na mesma static (RN32)
966 - [ ] `createReview()` -- character verificado, mesma static, 2 semanas, sem auto-review, sem duplicata (RN30-RN33)
967 - [ ] `updateReview()` -- apenas autor
968 - [ ] `getPlayerReviews()`, `getStaticReviews()`
969 - [ ] Evento ReviewCreatedEvent
970 - [ ] Verificacao de reputacao abaixo de 2.0 (RN36)
971 - [ ] Testes
972
973 ---
974
975 ### REP-04: Implementar ReputationService (calculo de media ponderada temporal)
976
977 **Labels:** `backend`, `reputation`, `priority:high`
978
979 **Checklist:**
980 - [ ] `getReputation()` -- media ponderada com peso temporal (RN35): peso = 1 / (1 + dias/180)
981 - [ ] Breakdown por criterio (pontualidade, comunicacao, performance, atitude, comprometimento)
982 - [ ] Calculo de tendencia (comparacao ultimo mes vs anterior)
983 - [ ] Cache Redis: `reputation:{targetId}:{type}` TTL 10min
984 - [ ] `isBelowThreshold()` -- usado pelo MatchingService (RN36)
985 - [ ] Testes do algoritmo de media ponderada
986
987 ---
988
989 ### REP-05: Implementar ReviewReportService e Controller
990
991 **Labels:** `backend`, `reputation`, `priority:medium`
992
993 **Checklist:**
994 - [ ] `reportReview()` -- sem auto-report, sem duplicata, alerta se 3+ reports (RN34)
995 - [ ] Controller com todos os endpoints (publicos para GET, autenticados para POST/PUT)
996 - [ ] Testes
997
998 ---
999
1000 ### REP-06: Frontend -- Componentes de reputacao
1001
1002 **Labels:** `frontend`, `reputation`, `priority:high`
1003
1004 **Checklist:**
1005 - [ ] Componentes: ReputationCard (score circular), ReputationChart (radar Recharts), ReviewList, ReviewCard, ReviewForm (5 sliders), ReportDialog
1006 - [ ] Integrar na pagina de perfil (`/users/[id]`) e pagina de static (`/statics/[id]`)
1007 - [ ] Hooks: useReputation, usePlayerReviews, useStaticReviews, useCreateReview, useReportReview
1008 - [ ] Schema Zod: createReviewSchema, createReportSchema
1009 - [ ] Instalar e configurar Recharts
1010
1011 ---
1012
1013 ## Milestone 7: Chat
1014
1015 > **Depende de:** Milestone 1, Milestone 3
1016 > **Referencia:** `docs/features/06-chat.md`
1017
1018 ### CHAT-01: Criar migration V7 (chat_rooms, chat_room_members, messages, message_reads)
1019
1020 **Labels:** `database`, `chat`, `priority:high`
1021
1022 **Checklist:**
1023 - [ ] Tabelas com UNIQUEs e suporte a soft delete de mensagens
1024 - [ ] Indices para busca por room, user, sent_at
1025 - [ ] Validar migration
1026
1027 ---
1028
1029 ### CHAT-02: Criar entities, repositories e DTOs
1030
1031 **Labels:** `backend`, `chat`, `priority:high`
1032
1033 **Checklist:**
1034 - [ ] `ChatRoom` entity com enum ChatRoomType (DM, STATIC)
1035 - [ ] `ChatRoomMember`, `Message`, `MessageRead` entities
1036 - [ ] Repositories com queries para rooms do usuario, DM entre dois users, mensagens paginadas, unread count
1037 - [ ] DTOs e ChatMapper
1038
1039 ---
1040
1041 ### CHAT-03: Configurar WebSocket (Spring + STOMP + SockJS)
1042
1043 **Labels:** `backend`, `chat`, `infra`, `priority:high`
1044
1045 **Checklist:**
1046 - [ ] `WebSocketConfig` -- registrar endpoint `/ws`, configurar STOMP broker
1047 - [ ] `ChannelInterceptor` -- validar JWT no CONNECT frame
1048 - [ ] Configurar origins permitidas para WebSocket
1049 - [ ] Testar conexao basica com cliente STOMP
1050
1051 ---
1052
1053 ### CHAT-04: Implementar ChatService e MessageService
1054
1055 **Labels:** `backend`, `chat`, `priority:high`
1056
1057 **Checklist:**
1058 - [ ] `getMyChatRooms()` -- lista com ultimo mensagem e unread count
1059 - [ ] `createDm()` -- verificacao de bloqueio (RN41), limite free 20 DMs (RN39), reutilizar DM existente
1060 - [ ] `createStaticChatRoom()` -- automatico ao criar static (RN38)
1061 - [ ] `addMemberToStaticChat()`, `removeMemberFromStaticChat()`, `deactivateStaticChat()`
1062 - [ ] `sendMessage()` -- sanitizacao XSS, broadcast STOMP, publicar no Redis Pub/Sub
1063 - [ ] `getMessages()` -- paginacao cursor-based
1064 - [ ] `markAsRead()` -- bulk update, broadcast read receipts
1065 - [ ] `broadcastTyping()` -- apenas broadcast, sem persistencia
1066 - [ ] Consumir eventos: StaticCreatedEvent, MemberJoinedEvent, MemberLeftEvent, StaticDissolvedEvent
1067 - [ ] Testes
1068
1069 ---
1070
1071 ### CHAT-05: Implementar ChatWebSocketController (STOMP)
1072
1073 **Labels:** `backend`, `chat`, `priority:high`
1074
1075 **Checklist:**
1076 - [ ] `@MessageMapping("/chat.send")` -- enviar mensagem
1077 - [ ] `@MessageMapping("/chat.typing")` -- indicador de digitacao
1078 - [ ] `@MessageMapping("/chat.read")` -- marcar como lido
1079 - [ ] Broadcast para `/topic/chat.{roomId}`
1080 - [ ] Testes
1081
1082 ---
1083
1084 ### CHAT-06: Implementar Redis Pub/Sub para multi-instance
1085
1086 **Labels:** `backend`, `chat`, `infra`, `priority:medium`
1087
1088 **Checklist:**
1089 - [ ] `ChatRedisConfig` -- RedisMessageListenerContainer
1090 - [ ] `ChatRedisPublisher` -- publica em `ws:chat:{roomId}`
1091 - [ ] `ChatRedisSubscriber` -- recebe e distribui para clients locais
1092 - [ ] Testes
1093
1094 ---
1095
1096 ### CHAT-07: Implementar PresenceService
1097
1098 **Labels:** `backend`, `chat`, `priority:medium`
1099
1100 **Checklist:**
1101 - [ ] `updatePresence()` -- heartbeat no Redis com TTL 5min
1102 - [ ] `isOnline()`, `getRoomPresence()`, `removePresence()`
1103 - [ ] Hook no connect/disconnect do WebSocket
1104 - [ ] Testes
1105
1106 ---
1107
1108 ### CHAT-08: Implementar ChatController (REST fallback)
1109
1110 **Labels:** `backend`, `chat`, `priority:high`
1111
1112 **Checklist:**
1113 - [ ] GET `/chats` -- listar conversas
1114 - [ ] POST `/chats/dm` -- iniciar DM
1115 - [ ] GET `/chats/{id}/messages` -- historico
1116 - [ ] POST `/chats/{id}/messages` -- enviar (fallback REST)
1117 - [ ] Testes
1118
1119 ---
1120
1121 ### CHAT-09: Frontend -- Paginas de chat
1122
1123 **Labels:** `frontend`, `chat`, `priority:high`
1124
1125 **Checklist:**
1126 - [ ] `/chat` -- layout split (lista + conversa)
1127 - [ ] `/chat/[id]` -- conversa ativa com scroll infinito
1128 - [ ] Componentes: ChatRoomList, ChatRoomItem, MessageList, MessageBubble, TypingIndicator, MessageInput
1129 - [ ] Zustand store `chat-store.ts` (activeChatRoomId, typingUsers, onlineUsers)
1130 - [ ] `hooks/use-websocket.ts` -- conexao STOMP com SockJS
1131 - [ ] `hooks/use-chat-websocket.ts` -- subscribe por room, sendMessage, sendTyping, markRead
1132 - [ ] `hooks/use-presence.ts` -- heartbeat de presenca
1133 - [ ] Hooks TanStack Query: useChatRooms, useChatMessages (infinite query), useCreateDm
1134 - [ ] Instalar @stomp/stompjs + sockjs-client
1135
1136 ---
1137
1138 ## Milestone 8: Notificacoes e Polimento
1139
1140 > **Depende de:** Milestones 1-7
1141 > **Referencia:** `docs/architecture.md` secao 5.2
1142
1143 ### NOTIF-01: Criar migration V9 e V10 (notifications, user_blocks)
1144
1145 **Labels:** `database`, `priority:medium`
1146
1147 **Checklist:**
1148 - [ ] `V9__create_notifications.sql`
1149 - [ ] `V10__create_user_blocks.sql`
1150 - [ ] `V11__create_indexes.sql` (indices adicionais globais)
1151
1152 ---
1153
1154 ### NOTIF-02: Implementar NotificationService
1155
1156 **Labels:** `backend`, `priority:medium`
1157
1158 **Checklist:**
1159 - [ ] Entity `Notification`
1160 - [ ] Consumir todos os eventos de dominio e gerar notificacoes in-app
1161 - [ ] Envio via WebSocket (`/user/queue/notifications`)
1162 - [ ] Envio de email para eventos criticos (verificacao, reset, pagamento)
1163 - [ ] Controller: GET /notifications, PUT /notifications/{id}/read, PUT /notifications/read-all
1164
1165 ---
1166
1167 ### NOTIF-03: Implementar UserBlockService
1168
1169 **Labels:** `backend`, `priority:medium`
1170
1171 **Checklist:**
1172 - [ ] Block/Unblock usuario
1173 - [ ] Integrar com ChatService (desativar DMs) e MatchingService (excluir de matches)
1174 - [ ] Controller: POST /users/block, DELETE /users/block/{userId}, GET /users/blocked
1175
1176 ---
1177
1178 ### NOTIF-04: Frontend -- Sistema de notificacoes
1179
1180 **Labels:** `frontend`, `priority:medium`
1181
1182 **Checklist:**
1183 - [ ] Componente NotificationBell no header com badge de contagem
1184 - [ ] Dropdown de notificacoes com lista e marcar como lida
1185 - [ ] Zustand store `notification-store.ts`
1186 - [ ] Hook `use-notifications.ts` (WebSocket subscription + REST queries)
1187 - [ ] Toast notifications via Sonner
1188
1189 ---
1190
1191 ### NOTIF-05: Frontend -- Dashboard
1192
1193 **Labels:** `frontend`, `priority:medium`
1194
1195 **Checklist:**
1196 - [ ] `/dashboard` -- pagina inicial apos login
1197 - [ ] Resumo de: statics ativas, proximos eventos, candidaturas pendentes, novos matches, notificacoes
1198 - [ ] Widgets: ProximoEventoCard, MatchesPendentesCard, CandidaturasPendentesCard
1199
1200 ---
1201
1202 ### NOTIF-06: Frontend -- Landing page (SSG)
1203
1204 **Labels:** `frontend`, `priority:low`
1205
1206 **Checklist:**
1207 - [ ] `/[locale]/page.tsx` -- landing page estatica
1208 - [ ] Hero section, Features section, How it works, Pricing, FAQ
1209 - [ ] SEO meta tags
1210 - [ ] SSG (gerado em build time)
1211
1212 ---
1213
1214 ## Milestone 9: Testes e Qualidade
1215
1216 > **Depende de:** Milestones 1-8
1217
1218 ### TEST-01: Testes unitarios backend (Services)
1219
1220 **Labels:** `backend`, `priority:high`
1221
1222 **Checklist:**
1223 - [ ] AuthServiceTest, ProfileServiceTest, CharacterServiceTest
1224 - [ ] StaticServiceTest, VacancyServiceTest, ApplicationServiceTest, InvitationServiceTest
1225 - [ ] MatchCalculationServiceTest, MatchingServiceTest
1226 - [ ] EventServiceTest, AttendanceServiceTest
1227 - [ ] ReviewServiceTest, CoexistenceValidatorTest, ReputationServiceTest
1228 - [ ] ChatServiceTest, MessageServiceTest
1229 - [ ] SubscriptionServiceTest
1230 - [ ] Cobertura >= 80%
1231
1232 ---
1233
1234 ### TEST-02: Testes de integracao backend (Controllers + DB)
1235
1236 **Labels:** `backend`, `priority:high`
1237
1238 **Checklist:**
1239 - [ ] AbstractIntegrationTest com TestContainers (PostgreSQL + Redis)
1240 - [ ] AuthControllerIntegrationTest (fluxo completo de registro/login/refresh)
1241 - [ ] StaticControllerIntegrationTest (criar static, vagas, candidaturas)
1242 - [ ] StripeWebhookControllerTest (simular webhooks)
1243 - [ ] Spring Security Test (verificar endpoints publicos/protegidos)
1244
1245 ---
1246
1247 ### TEST-03: Testes frontend (Vitest + React Testing Library)
1248
1249 **Labels:** `frontend`, `priority:high`
1250
1251 **Checklist:**
1252 - [ ] Testes de componentes: LoginForm, RegisterForm, StaticCard, MatchCard, ReviewForm
1253 - [ ] Testes de hooks: useAuth, useWebSocket
1254 - [ ] Testes de stores: auth-store, chat-store
1255 - [ ] Testes de schemas Zod
1256 - [ ] MSW para mock de API
1257 - [ ] Cobertura >= 75%
1258
1259 ---
1260
1261 ### TEST-04: Testes E2E (Playwright)
1262
1263 **Labels:** `frontend`, `priority:medium`
1264
1265 **Checklist:**
1266 - [ ] Fluxo de autenticacao (register -> verify -> login -> logout)
1267 - [ ] Fluxo de static (criar -> publicar vaga -> candidatar -> aceitar)
1268 - [ ] Fluxo de chat (criar DM -> enviar mensagem)
1269 - [ ] Fluxo de premium (selecionar plano -> checkout -> verificar status)
1270 - [ ] Configurar Playwright com ambiente de teste
1271
1272 ---
1273
1274 ## Ordem Recomendada de Desenvolvimento
1275
1276 `      1277 Milestone 0: Infraestrutura (INFRA-01 a INFRA-05)
      1278     |
      1279     v
      1280 Milestone 1: Auth & Perfil (AUTH-01 a AUTH-18)
      1281     |
      1282     +---> Milestone 2: Premium (PREMIUM-01 a PREMIUM-10)
      1283     |
      1284     v
      1285 Milestone 3: Statics (STATIC-01 a STATIC-12)
      1286     |
      1287     +---> Milestone 4: Matching (MATCH-01 a MATCH-06)
      1288     |
      1289     +---> Milestone 5: Calendario (CAL-01 a CAL-07)
      1290     |
      1291     +---> Milestone 6: Reputacao (REP-01 a REP-06)
      1292     |
      1293     +---> Milestone 7: Chat (CHAT-01 a CHAT-09)
      1294     |
      1295     v
      1296 Milestone 8: Notificacoes e Polimento (NOTIF-01 a NOTIF-06)
      1297     |
      1298     v
      1299 Milestone 9: Testes e Qualidade (TEST-01 a TEST-04)
      1300`
1301
