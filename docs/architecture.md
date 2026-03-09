# FFXIV Static Planner - Arquitetura do Sistema

## 1. Visao Geral

### 1.1 Stack Tecnologico

| Camada              | Tecnologia                        | Versao          |
| ------------------- | --------------------------------- | --------------- |
| **Frontend**        | Next.js (App Router)              | 15.x            |
| **Backend**         | Spring Boot (Java)                | 3.3.x / Java 21 |
| **Banco de Dados**  | PostgreSQL                        | 16.x            |
| **Cache / Broker**  | Redis                             | 7.x             |
| **Real-time**       | Spring WebSocket + SockJS + STOMP | -               |
| **Monorepo**        | Turborepo                         | 2.x             |
| **Containerizacao** | Docker + Docker Compose           | -               |

### 1.2 Diagrama de Arquitetura

```
                          +---------------------------+
                          |       CLIENTES            |
                          |  Browser / Mobile Web     |
                          +------------+--------------+
                                       |
                                       | HTTPS
                                       v
                          +---------------------------+
                          |     REVERSE PROXY         |
                          |        (Nginx)            |
                          +--+---------------------+--+
                             |                     |
                   /app/*    |                     |  /api/*
                   /static/* |                     |  /ws/*
                             v                     v
                +------------------+    +--------------------+
                |    FRONTEND      |    |     BACKEND        |
                |    Next.js       |    |    Spring Boot     |
                |    (SSR/CSR)     |    |                    |
                |                  |    | +----------------+ |
                | - App Router     |    | | REST API       | |
                | - React 19      |    | | Controllers    | |
                | - next-intl     |    | +----------------+ |
                | - TanStack Query|    | | Services       | |
                | - Zustand       |    | +----------------+ |
                | - Tailwind CSS  |    | | Repositories   | |
                | - ShadCN/UI     |    | | (Spring Data)  | |
                |                  |    | +----------------+ |
                +------------------+    | | WebSocket      | |
                                        | | (STOMP/SockJS) | |
                                        | +----------------+ |
                                        | | Scheduled Jobs | |
                                        | +----------------+ |
                                        +---------+----------+
                                                  |
                                  +---------------+---------------+
                                  |               |               |
                                  v               v               v
                          +------------+  +------------+  +----------------+
                          | PostgreSQL |  |   Redis    |  |   XIVAPI       |
                          |            |  |            |  |  (Lodestone)   |
                          | - Users    |  | - Cache    |  +----------------+
                          | - Statics  |  | - Sessions |
                          | - Events   |  | - Pub/Sub  |  +----------------+
                          | - Chat     |  | - Rate Lim |  |   Stripe       |
                          | - Reviews  |  |            |  |  (Payments)    |
                          +------------+  +------------+  +----------------+
```

---

## 2. Estrutura do Monorepo

```
ffxiv-static-planner/
|
+-- package.json                    # Root - Turborepo config
+-- turbo.json                      # Pipeline definitions
+-- docker-compose.yml              # PostgreSQL + Redis + Nginx
+-- .env.example                    # Variaveis de ambiente
+-- .gitignore
|
+-- apps/
|   +-- web/                        # Next.js Frontend
|   |   +-- package.json
|   |   +-- next.config.ts
|   |   +-- tailwind.config.ts
|   |   +-- tsconfig.json
|   |   +-- messages/               # i18n translation files
|   |   |   +-- en.json
|   |   |   +-- ja.json
|   |   |   +-- de.json
|   |   |   +-- fr.json
|   |   |   +-- pt.json
|   |   +-- public/
|   |   |   +-- images/
|   |   |   +-- icons/
|   |   +-- src/
|   |       +-- app/                 # App Router pages
|   |       |   +-- [locale]/        # i18n dynamic segment
|   |       |   |   +-- layout.tsx
|   |       |   |   +-- page.tsx                    # Landing
|   |       |   |   +-- (auth)/
|   |       |   |   |   +-- login/page.tsx
|   |       |   |   |   +-- register/page.tsx
|   |       |   |   |   +-- forgot-password/page.tsx
|   |       |   |   +-- (main)/                     # Layout autenticado
|   |       |   |   |   +-- layout.tsx
|   |       |   |   |   +-- dashboard/page.tsx
|   |       |   |   |   +-- profile/
|   |       |   |   |   |   +-- page.tsx
|   |       |   |   |   |   +-- edit/page.tsx
|   |       |   |   |   |   +-- characters/page.tsx
|   |       |   |   |   +-- statics/
|   |       |   |   |   |   +-- page.tsx            # Listar minhas statics
|   |       |   |   |   |   +-- create/page.tsx
|   |       |   |   |   |   +-- [id]/
|   |       |   |   |   |   |   +-- page.tsx        # Ver static
|   |       |   |   |   |   |   +-- manage/page.tsx
|   |       |   |   |   |   |   +-- applications/page.tsx
|   |       |   |   |   +-- search/
|   |       |   |   |   |   +-- statics/page.tsx
|   |       |   |   |   |   +-- players/page.tsx
|   |       |   |   |   +-- matches/
|   |       |   |   |   |   +-- page.tsx
|   |       |   |   |   |   +-- preferences/page.tsx
|   |       |   |   |   +-- calendar/
|   |       |   |   |   |   +-- page.tsx
|   |       |   |   |   |   +-- events/
|   |       |   |   |   |   |   +-- [id]/page.tsx
|   |       |   |   |   |   |   +-- create/page.tsx
|   |       |   |   |   |   +-- public/page.tsx     # Eventos PUG
|   |       |   |   |   +-- chat/
|   |       |   |   |   |   +-- page.tsx
|   |       |   |   |   |   +-- [id]/page.tsx
|   |       |   |   |   +-- premium/page.tsx
|   |       |   |   |   +-- account/
|   |       |   |   |       +-- subscription/page.tsx
|   |       |   |   |       +-- settings/page.tsx
|   |       |   +-- middleware.ts         # i18n redirect + auth guard
|   |       +-- components/
|   |       |   +-- ui/                   # ShadCN primitives
|   |       |   +-- auth/
|   |       |   +-- profile/
|   |       |   +-- static/
|   |       |   +-- search/
|   |       |   +-- matching/
|   |       |   +-- calendar/
|   |       |   +-- chat/
|   |       |   +-- reputation/
|   |       |   +-- premium/
|   |       |   +-- layout/               # Header, Sidebar, Footer
|   |       +-- hooks/
|   |       |   +-- use-auth.ts
|   |       |   +-- use-websocket.ts
|   |       |   +-- use-notifications.ts
|   |       +-- lib/
|   |       |   +-- api.ts                # Axios/fetch wrapper
|   |       |   +-- auth.ts               # JWT helpers
|   |       |   +-- websocket.ts          # SockJS + STOMP client
|   |       |   +-- utils.ts
|   |       +-- stores/
|   |       |   +-- auth-store.ts         # Zustand
|   |       |   +-- chat-store.ts
|   |       |   +-- notification-store.ts
|   |       +-- types/
|   |       |   +-- api.ts                # Response/Request types
|   |       |   +-- domain.ts             # Domain entities
|   |       +-- i18n/
|   |           +-- request.ts
|   |           +-- routing.ts
|   |
|   +-- api/                         # Spring Boot Backend
|       +-- build.gradle.kts         # Gradle config (Kotlin DSL)
|       +-- settings.gradle.kts
|       +-- gradle/
|       +-- src/
|           +-- main/
|           |   +-- java/com/ffxiv/planner/
|           |   |   +-- FfxivPlannerApplication.java
|           |   |   +-- config/
|           |   |   |   +-- SecurityConfig.java
|           |   |   |   +-- WebSocketConfig.java
|           |   |   |   +-- RedisConfig.java
|           |   |   |   +-- CorsConfig.java
|           |   |   |   +-- JacksonConfig.java
|           |   |   +-- auth/
|           |   |   |   +-- controller/
|           |   |   |   +-- service/
|           |   |   |   +-- repository/
|           |   |   |   +-- entity/
|           |   |   |   +-- dto/
|           |   |   |   +-- jwt/
|           |   |   +-- profile/
|           |   |   |   +-- controller/
|           |   |   |   +-- service/
|           |   |   |   +-- repository/
|           |   |   |   +-- entity/
|           |   |   |   +-- dto/
|           |   |   +-- static_/             # 'static' e reserved em Java
|           |   |   |   +-- controller/
|           |   |   |   +-- service/
|           |   |   |   +-- repository/
|           |   |   |   +-- entity/
|           |   |   |   +-- dto/
|           |   |   +-- matching/
|           |   |   |   +-- controller/
|           |   |   |   +-- service/
|           |   |   |   +-- repository/
|           |   |   |   +-- entity/
|           |   |   |   +-- dto/
|           |   |   |   +-- scheduler/
|           |   |   +-- calendar/
|           |   |   |   +-- controller/
|           |   |   |   +-- service/
|           |   |   |   +-- repository/
|           |   |   |   +-- entity/
|           |   |   |   +-- dto/
|           |   |   |   +-- scheduler/
|           |   |   +-- reputation/
|           |   |   |   +-- controller/
|           |   |   |   +-- service/
|           |   |   |   +-- repository/
|           |   |   |   +-- entity/
|           |   |   |   +-- dto/
|           |   |   +-- chat/
|           |   |   |   +-- controller/        # REST + WebSocket
|           |   |   |   +-- service/
|           |   |   |   +-- repository/
|           |   |   |   +-- entity/
|           |   |   |   +-- dto/
|           |   |   +-- premium/
|           |   |   |   +-- controller/
|           |   |   |   +-- service/
|           |   |   |   +-- repository/
|           |   |   |   +-- entity/
|           |   |   |   +-- dto/
|           |   |   |   +-- webhook/
|           |   |   +-- notification/
|           |   |   |   +-- service/
|           |   |   |   +-- entity/
|           |   |   |   +-- repository/
|           |   |   +-- integration/
|           |   |   |   +-- lodestone/
|           |   |   |   |   +-- LodestoneClient.java
|           |   |   |   |   +-- LodestoneMapper.java
|           |   |   |   +-- stripe/
|           |   |   |       +-- StripeService.java
|           |   |   |       +-- StripeWebhookController.java
|           |   |   +-- common/
|           |   |       +-- exception/
|           |   |       |   +-- GlobalExceptionHandler.java
|           |   |       |   +-- BusinessException.java
|           |   |       |   +-- ResourceNotFoundException.java
|           |   |       +-- dto/
|           |   |       |   +-- PageResponse.java
|           |   |       |   +-- ApiError.java
|           |   |       +-- util/
|           |   |       +-- audit/
|           |   |           +-- AuditableEntity.java
|           |   +-- resources/
|           |       +-- application.yml
|           |       +-- application-dev.yml
|           |       +-- application-prod.yml
|           |       +-- db/migration/             # Flyway
|           |           +-- V1__create_users.sql
|           |           +-- V2__create_characters.sql
|           |           +-- V3__create_statics.sql
|           |           +-- V4__create_matching.sql
|           |           +-- V5__create_calendar.sql
|           |           +-- V6__create_reputation.sql
|           |           +-- V7__create_chat.sql
|           |           +-- V8__create_premium.sql
|           +-- test/
|               +-- java/com/ffxiv/planner/
|
+-- packages/                        # Shared packages (opcional)
|   +-- eslint-config/
|   +-- typescript-config/
|
+-- docs/                            # Documentacao (ja existente)
    +-- business-domain.md
    +-- use-cases.md
    +-- architecture.md              # ESTE ARQUIVO
    +-- features/
        +-- 01-auth-profile.md
        +-- 02-static-management.md
        +-- 03-matching.md
        +-- 04-calendar.md
        +-- 05-reputation.md
        +-- 06-chat.md
        +-- 07-premium.md
```

---

## 3. Arquitetura do Frontend (Next.js)

### 3.1 Estrategia de Renderizacao

| Tipo de Pagina          | Estrategia   | Justificativa                                         |
| ----------------------- | ------------ | ----------------------------------------------------- |
| Landing page            | SSG (Static) | Conteudo estatico, maximo SEO                         |
| Login / Registro        | SSR          | Redirect se autenticado, SEO basico                   |
| Dashboard               | CSR          | Dados dinamicos do usuario, atualiza frequentemente   |
| Perfil publico          | SSR + ISR    | SEO importante, dados semi-estaticos (revalidate 60s) |
| Pagina da Static        | SSR + ISR    | SEO para recrutamento, revalidate 60s                 |
| Busca (statics/players) | CSR          | Filtros dinamicos, paginacao client-side              |
| Matching                | CSR          | Dados privados do usuario                             |
| Calendario              | CSR          | Interatividade alta, WebSocket updates                |
| Chat                    | CSR          | Real-time, WebSocket                                  |
| Premium / Checkout      | CSR          | Dados sensiveis, sem cache                            |

### 3.2 Gerenciamento de Estado

| Ferramenta                       | Uso                                                           |
| -------------------------------- | ------------------------------------------------------------- |
| **Zustand**                      | Estado global: autenticacao, notificacoes, tema, sidebar      |
| **TanStack Query (React Query)** | Cache de dados do servidor, mutations, invalidacao automatica |
| **React Hook Form + Zod**        | Formularios com validacao tipada                              |
| **URL state (nuqs)**             | Filtros de busca, paginacao, tabs                             |

### 3.3 Internacionalizacao (i18n)

- **Biblioteca**: `next-intl` (integracao nativa com App Router)
- **Roteamento**: `[locale]` como segmento dinamico (`/en/dashboard`, `/pt/dashboard`)
- **Middleware**: detecta locale do `Accept-Language` header e redireciona
- **Arquivos de traducao**: JSON por idioma em `/messages/{locale}.json`
- **Namespaces**: separados por feature (`auth.login.title`, `static.create.name`)
- **Termos FFXIV**: namespace especial `ffxiv.*` com termos oficiais de cada idioma

### 3.4 Bibliotecas do Frontend

| Categoria             | Biblioteca                              |
| --------------------- | --------------------------------------- |
| UI Components         | ShadCN/UI (Radix primitives + Tailwind) |
| Styling               | Tailwind CSS 4                          |
| HTTP Client           | Axios (com interceptors para JWT)       |
| Forms                 | React Hook Form + Zod                   |
| Calendar UI           | FullCalendar (React wrapper)            |
| Date/Time             | date-fns + date-fns-tz                  |
| WebSocket             | @stomp/stompjs + sockjs-client          |
| Charts (reputacao)    | Recharts                                |
| Toast/Notifications   | Sonner                                  |
| Icons                 | Lucide React                            |
| Markdown (descricoes) | react-markdown                          |

### 3.5 Autenticacao no Frontend

```
Fluxo JWT:
1. Login -> POST /api/auth/login -> recebe { accessToken, refreshToken }
2. accessToken armazenado em memoria (Zustand)
3. refreshToken armazenado em httpOnly cookie (set pelo backend)
4. Axios interceptor: injeta Authorization: Bearer {accessToken}
5. Axios interceptor: em 401, tenta POST /api/auth/refresh com cookie
6. Se refresh falha -> redireciona para /login
```

---

## 4. Arquitetura do Backend (Spring Boot)

### 4.1 Organizacao por Feature (Package by Feature)

Cada modulo segue a estrutura:

```
feature/
+-- controller/     # REST endpoints (@RestController)
+-- service/        # Logica de negocio (@Service)
+-- repository/     # Acesso a dados (Spring Data JPA)
+-- entity/         # Entidades JPA (@Entity)
+-- dto/            # Request/Response DTOs (Java Records)
+-- mapper/         # Conversao Entity <-> DTO (MapStruct)
+-- scheduler/      # Jobs agendados (quando aplicavel)
+-- event/          # Domain events (quando aplicavel)
```

### 4.2 Camadas e Responsabilidades

```
Controller (REST/WebSocket)
    |  - Validacao de entrada (@Valid + Bean Validation)
    |  - Mapeamento HTTP -> DTO
    |  - Autorizacao (@PreAuthorize)
    v
Service
    |  - Logica de negocio
    |  - Regras de negocio (RN01-RN45)
    |  - Orquestracao de repositorios
    |  - Publicacao de eventos (ApplicationEventPublisher)
    v
Repository (Spring Data JPA)
    |  - JPQL / Native queries
    |  - Specifications para busca dinamica
    |  - Projections para performance
    v
PostgreSQL
```

### 4.3 Spring Security - Configuracao JWT

```
Endpoints publicos (permitAll):
  POST   /api/auth/register
  POST   /api/auth/login
  POST   /api/auth/refresh
  POST   /api/auth/forgot-password
  GET    /api/statics/{id}           (perfil publico)
  GET    /api/users/{id}/profile     (perfil publico)
  GET    /api/events/public          (PUGs publicos)
  POST   /api/premium/webhook/stripe (webhook Stripe)

Endpoints autenticados (authenticated):
  Todos os demais /api/**

Endpoints com role especifica:
  PUT    /api/statics/{id}/manage    (STATIC_LEADER)
  DELETE /api/statics/{id}           (STATIC_LEADER)
  GET    /api/admin/**               (ADMIN)
```

### 4.4 Dependencias Spring Boot (build.gradle.kts)

```kotlin
dependencies {
    // Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.x")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.x")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.x")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Mapping
    implementation("org.mapstruct:mapstruct:1.6.x")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.x")

    // Stripe
    implementation("com.stripe:stripe-java:26.x")

    // HTTP Client (XIVAPI)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Scheduling
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // Dev
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

---

## 5. Banco de Dados (PostgreSQL)

### 5.1 Estrategia de Schema

- **Flyway** para versionamento de migrations
- **UUID** como primary key em todas as tabelas (type `uuid` nativo do PostgreSQL)
- **Audit columns** em todas as tabelas: `created_at`, `updated_at` (via JPA `@MappedSuperclass`)
- **Soft delete** para entidades criticas: `deleted_at` nullable (statics, users, reviews)
- **JSONB** para campos semi-estruturados: `content_scope[]`, `match_criteria{}`, `loot_rules`
- **Enums como VARCHAR**: armazenados como string para legibilidade e flexibilidade

### 5.2 Schema Resumido

```sql
-- Core
users (id, email, password_hash, locale, is_premium, premium_expires_at, ...)
characters (id, user_id, name, server, data_center, lodestone_id, is_verified, is_main, avatar_url, ...)
character_jobs (id, character_id, job_name, role, is_main, ilvl, parse_tier, progression, ...)

-- Statics
statics (id, name, description, content_scope JSONB, loot_rules, language, status, max_members, ...)
static_memberships (id, static_id, character_id, role, job_assigned, is_leader, joined_at, left_at, ...)
static_vacancies (id, static_id, role_needed, job_preferences JSONB, requirements JSONB, is_open, ...)
applications (id, vacancy_id, character_id, message, status, applied_at, responded_at, ...)
invitations (id, static_id, character_id, message, status, sent_at, responded_at, ...)

-- Matching
match_suggestions (id, character_id, target_type, target_id, score, criteria JSONB, status, ...)
match_preferences (id, character_id, criteria_weights JSONB, mandatory_filters JSONB, blacklist JSONB, ...)

-- Calendar
raid_events (id, static_id, title, description, content_type, encounter, start_time, end_time,
             timezone, is_recurring, recurrence_rule, is_public, max_participants, created_by, ...)
attendances (id, event_id, character_id, status, note, responded_at, ...)

-- Reputation
reviews (id, reviewer_id, target_id, target_type, punctuality, communication, performance,
         attitude, commitment, comment, created_at, ...)
review_reports (id, review_id, reporter_id, reason, details, status, resolved_at, ...)

-- Chat
chat_rooms (id, type, static_id, created_at, ...)
chat_room_members (id, chat_room_id, user_id, joined_at, ...)
messages (id, chat_room_id, sender_id, content, sent_at, ...)
message_reads (id, message_id, user_id, read_at, ...)

-- Premium
subscriptions (id, user_id, plan, status, start_date, end_date, stripe_subscription_id, auto_renew, ...)
payment_history (id, subscription_id, amount, currency, stripe_payment_id, status, paid_at, ...)

-- Notifications
notifications (id, user_id, type, title, body, link, is_read, created_at, ...)

-- System
user_blocks (id, blocker_id, blocked_id, created_at)
```

### 5.3 Estrategia de Indices

| Tabela               | Indice                               | Tipo          | Justificativa                 |
| -------------------- | ------------------------------------ | ------------- | ----------------------------- |
| `users`              | `email`                              | UNIQUE        | Login por email               |
| `characters`         | `lodestone_id`                       | UNIQUE        | Vinculo unico                 |
| `characters`         | `user_id`                            | BTREE         | Buscar personagens do usuario |
| `characters`         | `data_center, server`                | BTREE         | Filtro por DC/server          |
| `statics`            | `status, data_center`                | BTREE         | Busca de statics ativas       |
| `statics`            | `name`                               | GIN (trigram) | Busca textual por nome        |
| `static_memberships` | `static_id, character_id`            | UNIQUE        | Evitar duplicacao             |
| `static_memberships` | `character_id, left_at`              | BTREE         | Statics ativas de um jogador  |
| `static_vacancies`   | `static_id, is_open`                 | BTREE         | Vagas abertas por static      |
| `applications`       | `vacancy_id, status`                 | BTREE         | Candidaturas pendentes        |
| `applications`       | `character_id, status`               | BTREE         | Minhas candidaturas           |
| `match_suggestions`  | `character_id, status`               | BTREE         | Sugestoes ativas              |
| `raid_events`        | `static_id, start_time`              | BTREE         | Eventos proximos              |
| `raid_events`        | `is_public, data_center, start_time` | BTREE         | PUGs publicos                 |
| `reviews`            | `target_id, target_type`             | BTREE         | Reputacao do alvo             |
| `messages`           | `chat_room_id, sent_at`              | BTREE         | Historico de mensagens        |
| `notifications`      | `user_id, is_read, created_at`       | BTREE         | Notificacoes nao lidas        |

### 5.4 Flyway - Ordem de Migrations

```
V1__create_users_and_characters.sql
V2__create_statics_and_memberships.sql
V3__create_vacancies_applications_invitations.sql
V4__create_matching.sql
V5__create_calendar_events.sql
V6__create_reviews_reputation.sql
V7__create_chat.sql
V8__create_subscriptions_payments.sql
V9__create_notifications.sql
V10__create_user_blocks.sql
V11__create_indexes.sql
```

---

## 6. Cache (Redis)

### 6.1 Padroes de Uso

| Uso                    | Chave Redis                     | TTL     | Descricao                                         |
| ---------------------- | ------------------------------- | ------- | ------------------------------------------------- |
| **Sessao JWT**         | `session:{userId}`              | 30 dias | Armazena refresh token hash para invalidacao      |
| **Rate Limiting**      | `rate:{ip}:{endpoint}`          | 1 min   | Sliding window counter                            |
| **Matching Cache**     | `match:{characterId}`           | 24h     | Cache dos scores de match calculados              |
| **Perfil Cache**       | `profile:{characterId}`         | 5 min   | Cache de perfis publicos acessados frequentemente |
| **Lodestone Cache**    | `lodestone:{lodestoneId}`       | 1h      | Cache de dados importados do Lodestone            |
| **Contadores**         | `count:matches:{userId}:{week}` | 7 dias  | Controle de limite free (5/semana)                |
| **Contadores**         | `count:chats:{userId}`          | -       | Controle de limite free (20 DMs ativas)           |
| **WebSocket Pub/Sub**  | `ws:chat:{roomId}`              | -       | Canal pub/sub para mensagens de chat              |
| **WebSocket Presence** | `ws:presence:{userId}`          | 5 min   | Status online do usuario (heartbeat)              |
| **Lock Distribuido**   | `lock:matching:run`             | 10 min  | Evitar execucao concorrente do job de matching    |

### 6.2 Estrategia de Invalidacao

- **Write-through**: ao atualizar perfil, invalida `profile:{id}`
- **TTL-based**: matching e lodestone cache expiram naturalmente
- **Event-driven**: eventos de dominio disparam invalidacao (ex: membro adicionado -> invalida cache da static)

---

## 7. Real-time (WebSocket)

### 7.1 Arquitetura STOMP

```
Cliente (Browser)
    |
    |  SockJS (fallback: xhr-streaming, xhr-polling)
    v
Spring WebSocket Handler
    |
    |  STOMP Protocol
    v
SimpleBroker / Redis Broker
    |
    +-- /topic/chat.{roomId}          # Mensagens de chat
    +-- /topic/static.{staticId}      # Atualizacoes da static
    +-- /user/queue/notifications     # Notificacoes pessoais
    +-- /user/queue/matches           # Novas sugestoes de match
```

### 7.2 Endpoints STOMP

| Destination                 | Tipo      | Descricao                         |
| --------------------------- | --------- | --------------------------------- |
| `/app/chat.send`            | SEND      | Enviar mensagem de chat           |
| `/app/chat.typing`          | SEND      | Indicador de digitacao            |
| `/app/chat.read`            | SEND      | Marcar mensagens como lidas       |
| `/topic/chat.{roomId}`      | SUBSCRIBE | Receber mensagens de um chat room |
| `/user/queue/notifications` | SUBSCRIBE | Notificacoes pessoais do usuario  |
| `/user/queue/matches`       | SUBSCRIBE | Novas sugestoes de matching       |

### 7.3 Autenticacao WebSocket

1. Cliente conecta via SockJS passando JWT como query param
2. `ChannelInterceptor` valida o JWT no `CONNECT` frame
3. Se valido, `Principal` e setado na sessao WebSocket
4. Se invalido, conexao e rejeitada

### 7.4 Scaling com Redis Pub/Sub

Para suportar multiplas instancias do backend:

```
Instance A                       Instance B
    |                                |
    +--- Redis Pub/Sub channel ------+
         "ws:chat:{roomId}"

Quando Instance A recebe mensagem via REST/STOMP:
1. Salva no PostgreSQL
2. Publica no Redis channel
3. Todas as instances recebem e distribuem para seus WebSocket clients
```

---

## 8. Design da API REST

### 8.1 Convencoes

| Aspecto           | Convencao                                                  |
| ----------------- | ---------------------------------------------------------- |
| **Base URL**      | `/api/v1`                                                  |
| **Formato**       | JSON (application/json)                                    |
| **Naming**        | kebab-case para URLs, camelCase para JSON                  |
| **Paginacao**     | `?page=0&size=20&sort=createdAt,desc`                      |
| **Filtros**       | Query params: `?dataCenter=Primal&role=HEALER&status=OPEN` |
| **Versionamento** | URL path (`/api/v1/...`)                                   |
| **Datas**         | ISO 8601 com timezone (`2026-03-04T19:00:00Z`)             |
| **IDs**           | UUID v4                                                    |
| **Erros**         | RFC 7807 Problem Details                                   |

### 8.2 Formato de Resposta Padrao

**Sucesso (objeto):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Heavens' Gate",
  "status": "ACTIVE",
  "createdAt": "2026-03-04T19:00:00Z"
}
```

**Sucesso (lista paginada):**

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8,
  "hasNext": true
}
```

**Erro (RFC 7807):**

```json
{
  "type": "https://ffxiv-planner.com/errors/validation",
  "title": "Validation Error",
  "status": 422,
  "detail": "One or more fields failed validation",
  "instance": "/api/v1/statics",
  "errors": [
    {
      "field": "name",
      "message": "Name is required",
      "rejectedValue": null
    }
  ]
}
```

### 8.3 Mapa Completo de Endpoints

| Metodo | Endpoint                                   | Descricao                       | Auth                            |
| ------ | ------------------------------------------ | ------------------------------- | ------------------------------- |
|        | **Auth**                                   |                                 |                                 |
| POST   | `/api/v1/auth/register`                    | Registrar conta                 | Nao                             |
| POST   | `/api/v1/auth/login`                       | Login                           | Nao                             |
| POST   | `/api/v1/auth/refresh`                     | Renovar access token            | Cookie                          |
| POST   | `/api/v1/auth/logout`                      | Logout (invalidar refresh)      | Sim                             |
| POST   | `/api/v1/auth/forgot-password`             | Solicitar reset de senha        | Nao                             |
| POST   | `/api/v1/auth/reset-password`              | Redefinir senha                 | Nao (token)                     |
|        | **Profile**                                |                                 |                                 |
| GET    | `/api/v1/profile/me`                       | Meu perfil completo             | Sim                             |
| PUT    | `/api/v1/profile/me`                       | Atualizar meu perfil            | Sim                             |
| GET    | `/api/v1/users/{id}/profile`               | Perfil publico                  | Nao                             |
| POST   | `/api/v1/characters`                       | Adicionar personagem            | Sim                             |
| PUT    | `/api/v1/characters/{id}`                  | Atualizar personagem            | Sim                             |
| DELETE | `/api/v1/characters/{id}`                  | Remover personagem              | Sim                             |
| POST   | `/api/v1/characters/{id}/verify`           | Iniciar verificacao Lodestone   | Sim                             |
| POST   | `/api/v1/characters/{id}/verify/confirm`   | Confirmar verificacao           | Sim                             |
| POST   | `/api/v1/characters/{id}/jobs`             | Adicionar job                   | Sim                             |
| PUT    | `/api/v1/characters/{id}/jobs/{jobId}`     | Atualizar job                   | Sim                             |
| DELETE | `/api/v1/characters/{id}/jobs/{jobId}`     | Remover job                     | Sim                             |
|        | **Statics**                                |                                 |                                 |
| POST   | `/api/v1/statics`                          | Criar static                    | Sim                             |
| GET    | `/api/v1/statics/{id}`                     | Ver static                      | Nao                             |
| PUT    | `/api/v1/statics/{id}`                     | Atualizar static                | Sim (Leader)                    |
| DELETE | `/api/v1/statics/{id}`                     | Dissolver static                | Sim (Leader)                    |
| GET    | `/api/v1/statics/{id}/members`             | Listar membros                  | Nao                             |
| PUT    | `/api/v1/statics/{id}/members/{memberId}`  | Alterar membro                  | Sim (Leader)                    |
| DELETE | `/api/v1/statics/{id}/members/{memberId}`  | Remover membro                  | Sim (Leader)                    |
| POST   | `/api/v1/statics/{id}/transfer-leadership` | Transferir lideranca            | Sim (Leader)                    |
| POST   | `/api/v1/statics/{id}/leave`               | Sair da static                  | Sim (Member)                    |
|        | **Vacancies**                              |                                 |                                 |
| POST   | `/api/v1/statics/{id}/vacancies`           | Criar vaga                      | Sim (Leader)                    |
| PUT    | `/api/v1/statics/{id}/vacancies/{vacId}`   | Atualizar vaga                  | Sim (Leader)                    |
| DELETE | `/api/v1/statics/{id}/vacancies/{vacId}`   | Fechar vaga                     | Sim (Leader)                    |
| GET    | `/api/v1/statics/{id}/vacancies`           | Listar vagas                    | Nao                             |
|        | **Applications**                           |                                 |                                 |
| POST   | `/api/v1/vacancies/{vacId}/applications`   | Candidatar-se                   | Sim                             |
| GET    | `/api/v1/statics/{id}/applications`        | Listar candidaturas             | Sim (Leader)                    |
| GET    | `/api/v1/profile/me/applications`          | Minhas candidaturas             | Sim                             |
| PUT    | `/api/v1/applications/{appId}/accept`      | Aceitar candidatura             | Sim (Leader)                    |
| PUT    | `/api/v1/applications/{appId}/reject`      | Recusar candidatura             | Sim (Leader)                    |
| DELETE | `/api/v1/applications/{appId}`             | Cancelar candidatura            | Sim (Applicant)                 |
|        | **Invitations**                            |                                 |                                 |
| POST   | `/api/v1/statics/{id}/invitations`         | Convidar jogador                | Sim (Leader)                    |
| GET    | `/api/v1/profile/me/invitations`           | Meus convites recebidos         | Sim                             |
| PUT    | `/api/v1/invitations/{invId}/accept`       | Aceitar convite                 | Sim                             |
| PUT    | `/api/v1/invitations/{invId}/reject`       | Recusar convite                 | Sim                             |
| DELETE | `/api/v1/invitations/{invId}`              | Cancelar convite                | Sim (Leader)                    |
|        | **Search**                                 |                                 |                                 |
| GET    | `/api/v1/search/statics`                   | Buscar statics                  | Sim                             |
| GET    | `/api/v1/search/players`                   | Buscar jogadores                | Sim                             |
|        | **Matching**                               |                                 |                                 |
| GET    | `/api/v1/matches`                          | Minhas sugestoes                | Sim                             |
| GET    | `/api/v1/matches/{id}`                     | Detalhe do match                | Sim                             |
| PUT    | `/api/v1/matches/{id}/dismiss`             | Descartar match                 | Sim                             |
| GET    | `/api/v1/matches/preferences`              | Minhas preferencias             | Sim                             |
| PUT    | `/api/v1/matches/preferences`              | Atualizar preferencias          | Sim                             |
|        | **Calendar**                               |                                 |                                 |
| GET    | `/api/v1/statics/{id}/events`              | Eventos da static               | Sim (Member)                    |
| POST   | `/api/v1/statics/{id}/events`              | Criar evento                    | Sim (Leader)                    |
| PUT    | `/api/v1/events/{eventId}`                 | Atualizar evento                | Sim (Leader)                    |
| DELETE | `/api/v1/events/{eventId}`                 | Cancelar evento                 | Sim (Leader)                    |
| GET    | `/api/v1/events/public`                    | Eventos PUG publicos            | Nao                             |
| POST   | `/api/v1/events/public`                    | Criar evento PUG                | Sim (Premium)                   |
| PUT    | `/api/v1/events/{eventId}/attendance`      | Confirmar presenca              | Sim                             |
| POST   | `/api/v1/events/{eventId}/register`        | Inscrever-se em PUG             | Sim                             |
| DELETE | `/api/v1/events/{eventId}/register`        | Cancelar inscricao PUG          | Sim                             |
| GET    | `/api/v1/calendar/me`                      | Meu calendario consolidado      | Sim                             |
| GET    | `/api/v1/calendar/me/ical`                 | Exportar iCal                   | Sim (Premium)                   |
|        | **Reputation**                             |                                 |                                 |
| POST   | `/api/v1/reviews`                          | Criar avaliacao                 | Sim                             |
| PUT    | `/api/v1/reviews/{id}`                     | Editar avaliacao                | Sim (Author)                    |
| GET    | `/api/v1/users/{id}/reviews`               | Avaliacoes de um jogador        | Nao                             |
| GET    | `/api/v1/statics/{id}/reviews`             | Avaliacoes de uma static        | Nao                             |
| GET    | `/api/v1/users/{id}/reputation`            | Reputacao agregada              | Nao                             |
| POST   | `/api/v1/reviews/{id}/report`              | Reportar avaliacao              | Sim                             |
|        | **Chat**                                   |                                 |                                 |
| GET    | `/api/v1/chats`                            | Listar minhas conversas         | Sim                             |
| POST   | `/api/v1/chats/dm`                         | Iniciar conversa direta         | Sim                             |
| GET    | `/api/v1/chats/{id}/messages`              | Historico de mensagens          | Sim                             |
| POST   | `/api/v1/chats/{id}/messages`              | Enviar mensagem (REST fallback) | Sim                             |
|        | **Premium**                                |                                 |                                 |
| GET    | `/api/v1/subscriptions/plans`              | Listar planos                   | Nao                             |
| POST   | `/api/v1/subscriptions`                    | Iniciar assinatura              | Sim                             |
| GET    | `/api/v1/subscriptions/me`                 | Minha assinatura                | Sim                             |
| PUT    | `/api/v1/subscriptions/me/cancel`          | Cancelar assinatura             | Sim                             |
| PUT    | `/api/v1/subscriptions/me/reactivate`      | Reativar assinatura             | Sim                             |
| POST   | `/api/v1/subscriptions/trial`              | Ativar trial                    | Sim                             |
| GET    | `/api/v1/subscriptions/me/payments`        | Historico de pagamentos         | Sim                             |
| POST   | `/api/v1/webhooks/stripe`                  | Webhook Stripe                  | Nao (verificado por assinatura) |
|        | **Notifications**                          |                                 |                                 |
| GET    | `/api/v1/notifications`                    | Minhas notificacoes             | Sim                             |
| PUT    | `/api/v1/notifications/{id}/read`          | Marcar como lida                | Sim                             |
| PUT    | `/api/v1/notifications/read-all`           | Marcar todas como lidas         | Sim                             |
| GET    | `/api/v1/notifications/preferences`        | Preferencias de notificacao     | Sim                             |
| PUT    | `/api/v1/notifications/preferences`        | Atualizar preferencias          | Sim                             |
|        | **User Management**                        |                                 |                                 |
| POST   | `/api/v1/users/block`                      | Bloquear usuario                | Sim                             |
| DELETE | `/api/v1/users/block/{userId}`             | Desbloquear usuario             | Sim                             |
| GET    | `/api/v1/users/blocked`                    | Listar bloqueados               | Sim                             |

---

## 9. Seguranca

### 9.1 Autenticacao

| Aspecto           | Implementacao                                                 |
| ----------------- | ------------------------------------------------------------- |
| **Estrategia**    | JWT stateless                                                 |
| **Access Token**  | Validade 15 minutos, armazenado em memoria (frontend)         |
| **Refresh Token** | Validade 30 dias, httpOnly secure cookie                      |
| **Algoritmo**     | HS256 (HMAC-SHA256)                                           |
| **Rotacao**       | Refresh token rotacionado a cada uso (refresh token rotation) |
| **Revogacao**     | Blacklist de refresh tokens no Redis                          |
| **OAuth 2.0**     | Google e Discord como provedores adicionais                   |

### 9.2 Autorizacao

| Nivel          | Mecanismo                                               |
| -------------- | ------------------------------------------------------- |
| **Endpoint**   | `@PreAuthorize` com SpEL (Spring Expression Language)   |
| **Recurso**    | Verificacao no Service se o usuario e dono/lider/membro |
| **Premium**    | Aspect/Annotation customizada `@RequiresPremium`        |
| **Rate Limit** | Redis sliding window, configuravel por endpoint         |

### 9.3 Protecoes

| Ameaca                     | Protecao                                                                           |
| -------------------------- | ---------------------------------------------------------------------------------- |
| **CSRF**                   | Desabilitado para API stateless (JWT), habilitado para cookies com SameSite=Strict |
| **XSS**                    | Sanitizacao de input, Content-Security-Policy headers                              |
| **SQL Injection**          | Prepared statements via JPA, nenhuma query com concatenacao                        |
| **Brute Force**            | Rate limiting por IP + lockout de conta (5 tentativas = 15min lock)                |
| **Injection (NoSQL/LDAP)** | Nao aplicavel (PostgreSQL + JPA)                                                   |
| **Mass Assignment**        | DTOs explicitos, nenhum bind direto em entidades                                   |

---

## 10. Jobs Agendados (Scheduled)

| Job                   | Frequencia                  | Descricao                                                                    |
| --------------------- | --------------------------- | ---------------------------------------------------------------------------- |
| `MatchingJob`         | Diario (03:00 UTC)          | Recalcula sugestoes de match para todos os jogadores LFS e statics com vagas |
| `ReminderJob`         | A cada 15 minutos           | Verifica eventos proximos e envia lembretes (24h e 1h antes)                 |
| `ExpirationJob`       | Diario (00:00 UTC)          | Expira candidaturas (14 dias) e convites (7 dias) sem resposta               |
| `SubscriptionJob`     | Diario (01:00 UTC)          | Verifica assinaturas expirando, envia alertas, executa downgrade             |
| `LodestoneRefreshJob` | Semanal (domingo 05:00 UTC) | Resincroniza dados do Lodestone para personagens verificados                 |
| `CleanupJob`          | Semanal                     | Remove notificacoes lidas com mais de 90 dias, sessoes expiradas             |

---

## 11. Containerizacao (Docker)

### 11.1 docker-compose.yml (desenvolvimento)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ffxiv_planner
      POSTGRES_USER: planner
      POSTGRES_PASSWORD: planner_dev
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  api:
    build: ./apps/api
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ffxiv_planner
      SPRING_REDIS_HOST: redis
    depends_on:
      - postgres
      - redis

  web:
    build: ./apps/web
    ports:
      - "3000:3000"
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8080/api/v1
      NEXT_PUBLIC_WS_URL: http://localhost:8080/ws

volumes:
  pgdata:
```

### 11.2 Dockerfiles

**apps/api/Dockerfile (multi-stage):**

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**apps/web/Dockerfile (multi-stage):**

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:22-alpine
WORKDIR /app
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
EXPOSE 3000
CMD ["node", "server.js"]
```

---

## 12. Observabilidade

| Aspecto             | Ferramenta                                |
| ------------------- | ----------------------------------------- |
| **Logging**         | SLF4J + Logback (JSON format em producao) |
| **Metricas**        | Spring Actuator + Micrometer + Prometheus |
| **Health Check**    | `/actuator/health` (liveness + readiness) |
| **Tracing**         | Spring Observation (Micrometer Tracing)   |
| **Frontend Errors** | Sentry (React Error Boundary)             |

---

## 13. Decisoes Arquiteturais (ADRs Resumidos)

| ADR         | Decisao                                | Justificativa                                                                         |
| ----------- | -------------------------------------- | ------------------------------------------------------------------------------------- |
| **ADR-001** | Monorepo com Turborepo                 | Permite compartilhar types/configs, CI unificado, versionamento atomico               |
| **ADR-002** | Package by Feature (backend)           | Maior coesao, boundaries claros entre modulos, facilita futuro split em microservicos |
| **ADR-003** | JWT stateless + refresh token rotation | Escalabilidade horizontal sem sessao compartilhada, seguranca com rotacao             |
| **ADR-004** | STOMP sobre SockJS                     | Protocolo maduro, suporte nativo Spring, fallback para ambientes sem WebSocket        |
| **ADR-005** | Redis Pub/Sub para WebSocket           | Permite multiplas instancias do backend sem perder mensagens                          |
| **ADR-006** | Flyway (nao JPA auto-ddl)              | Controle total do schema, migrations auditaveis, rollback seguro                      |
| **ADR-007** | MapStruct (nao ModelMapper)            | Geracao em compile-time, type-safe, sem reflection, melhor performance                |
| **ADR-008** | TanStack Query (nao SWR)               | Devtools superior, mutations com cache invalidation, melhor para apps complexos       |
| **ADR-009** | ShadCN/UI (nao Material/Chakra)        | Componentes copiaveis (ownership), Tailwind nativo, sem bundle overhead               |
| **ADR-010** | next-intl (nao next-i18next)           | Suporte nativo App Router, RSC-compatible, API moderna                                |

---

## 14. Diagrama C4

### 14.1 Nivel 1 - Contexto do Sistema

```
+---------------------+
|    Jogador FFXIV     |
|  (Browser/Mobile)    |
+----------+----------+
           |
           | HTTPS
           v
+---------------------+
| FFXIV Static Planner |
|   (Web Application)  |
+--+--------+--------+-+
   |        |        |
   v        v        v
+------+ +------+ +--------+
|Lodes-| |Stripe| |Google/ |
|tone/ | |(Pay- | |Discord |
|XIVAPI| |ments)| |(OAuth) |
+------+ +------+ +--------+
```

**Descricao:**

| Elemento             | Tipo            | Descricao                                                          |
| -------------------- | --------------- | ------------------------------------------------------------------ |
| Jogador FFXIV        | Pessoa          | Usuario da plataforma (jogador, lider de static, PUG organizer)    |
| FFXIV Static Planner | Sistema         | Plataforma web para recrutamento, formacao e gestao de statics     |
| Lodestone/XIVAPI     | Sistema Externo | API do FFXIV para verificacao de personagens e importacao de dados |
| Stripe               | Sistema Externo | Gateway de pagamento para assinaturas premium                      |
| Google/Discord       | Sistema Externo | Provedores OAuth 2.0 para autenticacao alternativa                 |

### 14.2 Nivel 2 - Containers

```
+----------------------------------------------------------+
|                    FFXIV Static Planner                   |
|                                                          |
|  +-----------+    +-------------+    +--------------+    |
|  |           |    |             |    |              |    |
|  |  Nginx    +--->+  Frontend   |    |   Backend    |    |
|  | (Reverse  |    |  (Next.js)  |    | (Spring Boot)|    |
|  |  Proxy)   +--->+             |    |              |    |
|  |           |    |  - SSR/CSR  |    | - REST API   |    |
|  +-----------+    |  - React 19 |    | - WebSocket  |    |
|                   |  - i18n     |    | - Jobs       |    |
|                   +-----------+-+    +-+----+-------+    |
|                               |        |    |            |
|                               |   +----+    |            |
|                               |   |         |            |
|                           +---v---v-+  +----v-----+      |
|                           |PostgreSQL|  |  Redis   |      |
|                           |  16.x   |  |  7.x     |      |
|                           |         |  |          |      |
|                           | - Users |  | - Cache  |      |
|                           | - Data  |  | - Pub/Sub|      |
|                           | - Chat  |  | - Sessions|     |
|                           +---------+  +----------+      |
+----------------------------------------------------------+
```

| Container      | Tecnologia               | Responsabilidade                                           |
| -------------- | ------------------------ | ---------------------------------------------------------- |
| **Nginx**      | Nginx                    | Reverse proxy, SSL termination, roteamento /app vs /api    |
| **Frontend**   | Next.js 15, React 19     | Interface do usuario, SSR/CSR, i18n                        |
| **Backend**    | Spring Boot 3.3, Java 21 | API REST, WebSocket, logica de negocio, jobs agendados     |
| **PostgreSQL** | PostgreSQL 16            | Persistencia de dados (schema relacional, JSONB)           |
| **Redis**      | Redis 7                  | Cache, sessoes, pub/sub, rate limiting, locks distribuidos |

### 14.3 Nivel 3 - Componentes do Backend

```
+-------------------------------------------------------------------+
|                      Spring Boot Backend                          |
|                                                                   |
|  +----------+  +----------+  +----------+  +----------+          |
|  |   Auth   |  | Profile  |  | Static_  |  | Matching |          |
|  |          |  |          |  |          |  |          |          |
|  | - JWT    |  | - CRUD   |  | - CRUD   |  | - Score  |          |
|  | - OAuth  |  | - Lodes. |  | - Vagas  |  | - Job    |          |
|  | - Refresh|  | - Jobs   |  | - Aplic. |  | - Prefs  |          |
|  +----+-----+  +----+-----+  +----+-----+  +----+-----+          |
|       |              |              |              |              |
|  +----+-----+  +----+-----+  +----+-----+  +----+-----+          |
|  | Calendar |  | Reputat. |  |   Chat   |  | Premium  |          |
|  |          |  |          |  |          |  |          |          |
|  | - Events |  | - Reviews|  | - DM     |  | - Stripe |          |
|  | - Attend.|  | - Reports|  | - STOMP  |  | - Subs   |          |
|  | - iCal   |  | - Score  |  | - Pub/Sub|  | - Webhook|          |
|  +----+-----+  +----+-----+  +----+-----+  +----+-----+          |
|       |              |              |              |              |
|  +----+-----+  +----+-----+  +-----------+                       |
|  | Notificat|  | Integrat.|  |  Common   |                       |
|  |          |  |          |  |           |                       |
|  | - Email  |  | - Lodes. |  | - Except. |                       |
|  | - Push   |  | - Stripe |  | - Audit   |                       |
|  | - In-app |  |          |  | - DTOs    |                       |
|  +----------+  +----------+  +-----------+                       |
+-------------------------------------------------------------------+
```

**Comunicacao entre modulos**: via `ApplicationEventPublisher` (eventos de dominio).
Cada modulo e independente e se comunica apenas via eventos publicados e interfaces de servico.

---

## 15. Estrategia de Testes

### 15.1 Backend (Java/Spring Boot)

| Nivel          | Framework                         | Escopo                                        | Cobertura Alvo     |
| -------------- | --------------------------------- | --------------------------------------------- | ------------------ |
| **Unitario**   | JUnit 5 + Mockito                 | Services, Mappers, Validators, algoritmos     | >= 80%             |
| **Integracao** | Spring Boot Test + TestContainers | Repositories, Controllers, fluxos completos   | >= 70%             |
| **Seguranca**  | Spring Security Test              | Autenticacao JWT, autorizacao por role        | 100% dos endpoints |
| **WebSocket**  | Spring WebSocket Test             | Conexao STOMP, envio/recebimento de mensagens | Principais fluxos  |

#### Estrutura de Testes Backend

```
src/test/java/com/ffxiv/planner/
+-- auth/
|   +-- service/AuthServiceTest.java          # Unitario
|   +-- service/AuthServiceIntegrationTest.java # Integracao (TestContainers)
|   +-- controller/AuthControllerTest.java    # MockMvc
+-- profile/
|   +-- service/ProfileServiceTest.java
|   +-- service/CharacterServiceTest.java
+-- static_/
|   +-- service/StaticServiceTest.java
|   +-- service/ApplicationServiceTest.java
+-- matching/
|   +-- service/MatchCalculationServiceTest.java  # Algoritmo isolado
|   +-- scheduler/MatchSchedulerTest.java
+-- calendar/
|   +-- service/EventServiceTest.java
|   +-- service/AttendanceServiceTest.java
+-- reputation/
|   +-- service/ReviewServiceTest.java
|   +-- service/CoexistenceValidatorTest.java
+-- chat/
|   +-- service/ChatServiceTest.java
|   +-- controller/ChatWebSocketControllerTest.java
+-- premium/
|   +-- service/SubscriptionServiceTest.java
|   +-- webhook/StripeWebhookControllerTest.java
+-- common/
    +-- AbstractIntegrationTest.java          # Base com TestContainers
```

#### TestContainers (Integracao)

```java
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("ffxiv_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

### 15.2 Frontend (Next.js/React)

| Nivel          | Framework                          | Escopo                            | Cobertura Alvo                  |
| -------------- | ---------------------------------- | --------------------------------- | ------------------------------- |
| **Unitario**   | Vitest + React Testing Library     | Componentes, hooks, utils, stores | >= 80%                          |
| **Integracao** | Vitest + MSW (Mock Service Worker) | Fluxos com API mockada            | Principais fluxos               |
| **E2E**        | Playwright                         | Fluxos criticos end-to-end        | Fluxos de usuario               |
| **Visual**     | Storybook (opcional)               | Componentes UI isolados           | Componentes ShadCN customizados |

#### Estrutura de Testes Frontend

```
apps/web/
+-- __tests__/
|   +-- components/
|   |   +-- auth/LoginForm.test.tsx
|   |   +-- static/StaticCard.test.tsx
|   |   +-- chat/MessageBubble.test.tsx
|   +-- hooks/
|   |   +-- use-auth.test.ts
|   |   +-- use-websocket.test.ts
|   +-- stores/
|   |   +-- auth-store.test.ts
|   |   +-- chat-store.test.ts
|   +-- lib/
|       +-- api.test.ts
+-- e2e/
    +-- auth.spec.ts                # Login, registro, forgot password
    +-- static-management.spec.ts   # CRUD de static, vagas, candidaturas
    +-- chat.spec.ts                # Envio de mensagens, DMs
    +-- premium.spec.ts             # Checkout, cancelamento
```

#### Testes E2E (Playwright)

```typescript
// e2e/auth.spec.ts
test.describe("Authentication", () => {
  test("should register a new account", async ({ page }) => {
    await page.goto("/en/register");
    await page.fill('[name="email"]', "test@example.com");
    await page.fill('[name="password"]', "TestPass123");
    await page.fill('[name="confirmPassword"]', "TestPass123");
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/verify/);
  });

  test("should login with valid credentials", async ({ page }) => {
    await page.goto("/en/login");
    await page.fill('[name="email"]', "existing@example.com");
    await page.fill('[name="password"]', "ValidPass123");
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/dashboard/);
  });
});
```

### 15.3 Cobertura Minima

| Modulo            | Backend          | Frontend   |
| ----------------- | ---------------- | ---------- |
| Auth/Profile      | 90%              | 80%        |
| Static Management | 85%              | 75%        |
| Matching          | 90% (algoritmo)  | 70%        |
| Calendar          | 80%              | 70%        |
| Reputation        | 85%              | 70%        |
| Chat              | 80%              | 70%        |
| Premium           | 90% (pagamentos) | 75%        |
| **Global**        | **>= 80%**       | **>= 75%** |

---

## 16. Pipeline CI/CD

### 16.1 GitHub Actions

#### PR Validation

```yaml
# .github/workflows/pr-validation.yml
name: PR Validation
on:
  pull_request:
    branches: [main, develop]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 22 }
      - run: npm ci
      - run: npm run lint

  test-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 22 }
      - run: npm ci
      - run: cd apps/web && npm run test -- --coverage
      - uses: actions/upload-artifact@v4
        with:
          name: frontend-coverage
          path: apps/web/coverage/

  test-backend:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: ffxiv_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports: ["5432:5432"]
      redis:
        image: redis:7-alpine
        ports: ["6379:6379"]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - run: cd apps/api && ./gradlew test jacocoTestReport
      - uses: actions/upload-artifact@v4
        with:
          name: backend-coverage
          path: apps/api/build/reports/jacoco/

  build:
    needs: [lint, test-frontend, test-backend]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 22 }
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - run: npm ci && npm run build
      - run: cd apps/api && ./gradlew bootJar
```

#### Staging Deploy

```yaml
# .github/workflows/staging-deploy.yml
name: Deploy to Staging
on:
  push:
    branches: [develop]

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker images
        run: |
          docker build -t ffxiv-api:staging ./apps/api
          docker build -t ffxiv-web:staging ./apps/web
      - name: Push to registry
        run: |
          docker tag ffxiv-api:staging ${{ secrets.REGISTRY }}/ffxiv-api:staging
          docker tag ffxiv-web:staging ${{ secrets.REGISTRY }}/ffxiv-web:staging
          docker push ${{ secrets.REGISTRY }}/ffxiv-api:staging
          docker push ${{ secrets.REGISTRY }}/ffxiv-web:staging
      - name: Deploy to staging
        run: |
          # Deploy via Docker Compose, Kubernetes, ou plataforma escolhida
```

#### Production Deploy

```yaml
# .github/workflows/production-deploy.yml
name: Deploy to Production
on:
  push:
    tags: ["v*"]

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run E2E tests against staging
        run: npx playwright test --project=staging

  deploy:
    needs: [e2e]
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4
      - name: Build and push production images
        run: |
          docker build -t ffxiv-api:${{ github.ref_name }} ./apps/api
          docker build -t ffxiv-web:${{ github.ref_name }} ./apps/web
          docker push ${{ secrets.REGISTRY }}/ffxiv-api:${{ github.ref_name }}
          docker push ${{ secrets.REGISTRY }}/ffxiv-web:${{ github.ref_name }}
      - name: Deploy to production
        run: |
          # Blue-green ou rolling deployment
```

### 16.2 Stages do Pipeline

```
PR aberto
    |
    v
[Lint] --> [Test Frontend] --> [Test Backend] --> [Build]
    |                                                |
    v                                                v
Merge em develop                              Artifacts prontos
    |
    v
[Build Docker] --> [Push Registry] --> [Deploy Staging]
    |
    v
E2E em Staging
    |
    v
Tag v*.*.* (manual)
    |
    v
[E2E Production Check] --> [Build Prod Images] --> [Deploy Production]
```

---

## 17. Guia de Deployment

### 17.1 Ambientes

| Ambiente        | Branch/Tag      | URL                         | Descricao                                |
| --------------- | --------------- | --------------------------- | ---------------------------------------- |
| **Development** | `feature/*`     | `localhost`                 | Desenvolvimento local com Docker Compose |
| **Staging**     | `develop`       | `staging.ffxiv-planner.com` | Ambiente de pre-producao                 |
| **Production**  | `v*.*.*` (tags) | `ffxiv-planner.com`         | Ambiente de producao                     |

### 17.2 Variaveis de Ambiente

#### Backend (Spring Boot)

| Variavel                      | Descricao                     | Exemplo                                     |
| ----------------------------- | ----------------------------- | ------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`      | Perfil ativo                  | `dev`, `staging`, `prod`                    |
| `SPRING_DATASOURCE_URL`       | URL do PostgreSQL             | `jdbc:postgresql://host:5432/ffxiv_planner` |
| `SPRING_DATASOURCE_USERNAME`  | Usuario do DB                 | `planner`                                   |
| `SPRING_DATASOURCE_PASSWORD`  | Senha do DB                   | (secret)                                    |
| `SPRING_DATA_REDIS_HOST`      | Host do Redis                 | `redis`                                     |
| `SPRING_DATA_REDIS_PORT`      | Porta do Redis                | `6379`                                      |
| `SPRING_DATA_REDIS_PASSWORD`  | Senha do Redis                | (secret, se aplicavel)                      |
| `APP_JWT_SECRET`              | Secret key JWT (min 256 bits) | (secret)                                    |
| `APP_JWT_ACCESS_EXPIRATION`   | TTL access token (ms)         | `900000`                                    |
| `APP_JWT_REFRESH_EXPIRATION`  | TTL refresh token (ms)        | `2592000000`                                |
| `STRIPE_SECRET_KEY`           | Chave secreta do Stripe       | `sk_live_...`                               |
| `STRIPE_WEBHOOK_SECRET`       | Secret do webhook Stripe      | `whsec_...`                                 |
| `STRIPE_PRICE_MONTHLY`        | Price ID do plano mensal      | `price_...`                                 |
| `STRIPE_PRICE_ANNUAL`         | Price ID do plano anual       | `price_...`                                 |
| `XIVAPI_BASE_URL`             | URL da XIVAPI                 | `https://xivapi.com`                        |
| `XIVAPI_KEY`                  | Chave da XIVAPI               | (secret)                                    |
| `MAIL_HOST`                   | SMTP host                     | `smtp.example.com`                          |
| `MAIL_PORT`                   | SMTP port                     | `587`                                       |
| `MAIL_USERNAME`               | SMTP usuario                  | (secret)                                    |
| `MAIL_PASSWORD`               | SMTP senha                    | (secret)                                    |
| `APP_CORS_ORIGINS`            | Origens CORS permitidas       | `https://ffxiv-planner.com`                 |
| `OAUTH_GOOGLE_CLIENT_ID`      | Google OAuth client ID        | (secret)                                    |
| `OAUTH_GOOGLE_CLIENT_SECRET`  | Google OAuth secret           | (secret)                                    |
| `OAUTH_DISCORD_CLIENT_ID`     | Discord OAuth client ID       | (secret)                                    |
| `OAUTH_DISCORD_CLIENT_SECRET` | Discord OAuth secret          | (secret)                                    |

#### Frontend (Next.js)

| Variavel                             | Descricao            | Exemplo                                |
| ------------------------------------ | -------------------- | -------------------------------------- |
| `NEXT_PUBLIC_API_URL`                | URL da API backend   | `https://api.ffxiv-planner.com/api/v1` |
| `NEXT_PUBLIC_WS_URL`                 | URL do WebSocket     | `wss://api.ffxiv-planner.com/ws`       |
| `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` | Chave publica Stripe | `pk_live_...`                          |
| `NEXT_PUBLIC_SENTRY_DSN`             | DSN do Sentry        | `https://...@sentry.io/...`            |
| `NEXT_PUBLIC_GA_ID`                  | Google Analytics ID  | `G-XXXXXXXXXX`                         |

### 17.3 Health Checks e Readiness Probes

#### Backend

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  health:
    db:
      enabled: true
    redis:
      enabled: true
```

| Endpoint                     | Tipo      | Descricao                                     |
| ---------------------------- | --------- | --------------------------------------------- |
| `/actuator/health/liveness`  | Liveness  | Aplicacao esta rodando                        |
| `/actuator/health/readiness` | Readiness | Aplicacao esta pronta (DB e Redis conectados) |
| `/actuator/health`           | Health    | Detalhes de saude (DB, Redis, disk)           |
| `/actuator/prometheus`       | Metricas  | Metricas para Prometheus                      |

#### Frontend

| Endpoint      | Tipo              | Descricao                  |
| ------------- | ----------------- | -------------------------- |
| `/api/health` | Next.js API Route | Retorna `{ status: "ok" }` |

### 17.4 Nginx (Producao)

```nginx
upstream frontend {
    server web:3000;
}

upstream backend {
    server api:8080;
}

server {
    listen 443 ssl http2;
    server_name ffxiv-planner.com;

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    # Frontend
    location / {
        proxy_pass http://frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Backend API
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket
    location /ws/ {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400;
    }

    # Actuator (restrito)
    location /actuator/ {
        proxy_pass http://backend;
        allow 10.0.0.0/8;
        deny all;
    }
}
```

---

## 18. Indice de Features

### 18.1 Documentacao Tecnica por Feature

| #   | Feature               | Documento                                                   | Modulos Backend             | UCs       | RNs       |
| --- | --------------------- | ----------------------------------------------------------- | --------------------------- | --------- | --------- |
| 01  | Autenticacao & Perfil | [01-auth-profile.md](features/01-auth-profile.md)           | auth, profile               | UC01-UC04 | RN01-RN05 |
| 02  | Gestao de Statics     | [02-static-management.md](features/02-static-management.md) | static\_, busca             | UC05-UC11 | RN06-RN18 |
| 03  | Matching              | [03-matching.md](features/03-matching.md)                   | matching                    | UC12-UC14 | RN19-RN23 |
| 04  | Calendario            | [04-calendar.md](features/04-calendar.md)                   | calendar                    | UC15-UC19 | RN24-RN29 |
| 05  | Reputacao             | [05-reputation.md](features/05-reputation.md)               | reputation                  | UC20-UC22 | RN30-RN36 |
| 06  | Chat                  | [06-chat.md](features/06-chat.md)                           | chat                        | UC23-UC25 | RN37-RN41 |
| 07  | Premium               | [07-premium.md](features/07-premium.md)                     | premium, integration/stripe | UC26-UC28 | RN42-RN45 |

### 18.2 Mapa de Dependencias entre Modulos

```
                    +------------------+
                    | auth-profile     |
                    | (User, Character)|
                    +--------+---------+
                             |
              +--------------+--------------+
              |              |              |
              v              v              v
    +---------+----+  +------+-------+  +---+---------+
    | static-mgmt  |  |   premium   |  | notification|
    | (Static,     |  | (Subscript.)|  | (Email,Push)|
    | Vacancy,App) |  +------+-------+  +---+---------+
    +---+---+------+         |              ^
        |   |                |              |
   +----+   +------+--------+---+----------+
   |        |      |            |
   v        v      v            |
+--+---+ +--+---+ +--+------+  |
|match | |calen | |reputat. |  |
|(Score| |(Event| |(Review, |  |
| Job) | | Att.)| | Score)  |  |
+------+ +------+ +---------+  |
                                |
              +---------+       |
              |  chat   +-------+
              | (DM,    |
              |  STOMP) |
              +---------+
```

**Legenda:**

- Seta indica dependencia (o modulo de origem depende do destino)
- `auth-profile` e a base de todos os modulos (fornece User e Character)
- `premium` e transversal (todos os modulos consultam limites free/premium)
- `notification` e consumido por todos os modulos via eventos
- Comunicacao entre modulos ocorre via `ApplicationEventPublisher` (eventos de dominio)

### 18.3 Documentacao Complementar

| Documento                                | Descricao                                                                         |
| ---------------------------------------- | --------------------------------------------------------------------------------- |
| [business-domain.md](business-domain.md) | Regras de negocio (RN01-RN45), modelo de dominio, personas, glossario FFXIV       |
| [use-cases.md](use-cases.md)             | Casos de uso detalhados (UC01-UC28) com fluxos, excecoes e criterios de aceitacao |
| [architecture.md](architecture.md)       | Arquitetura geral do sistema (este documento)                                     |
