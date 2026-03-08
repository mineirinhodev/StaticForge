# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FFXIV Static Planner is a full-stack web platform for Final Fantasy XIV players to recruit, form, and manage raid statics (fixed groups). Features include bidirectional recruitment, automated matching, shared calendar, reputation system, real-time chat, and freemium monetization with Stripe.

**Language rule:** Documentation in `docs/` is written in Portuguese (Brazilian). All code, comments, commit messages, and user-facing strings must be in English.

## Build & Development Commands

```bash
# Infrastructure
docker compose up postgres redis        # Start PostgreSQL + Redis
docker compose up                        # Full environment (api, web, postgres, redis, nginx)

# Development (from root)
npm run dev                              # All apps via Turborepo
npm run build                            # Build all
npm run lint                             # Lint all
npm run test                             # Test all

# Frontend (apps/web)
cd apps/web && npm run dev               # Dev server on :3000

# Backend (apps/api)
cd apps/api && ./gradlew bootRun         # Dev server on :8080
cd apps/api && ./gradlew test            # Run all backend tests
cd apps/api && ./gradlew test --tests "com.ffxiv.planner.auth.service.AuthServiceTest"  # Single test class
cd apps/api && ./gradlew test --tests "*.AuthServiceTest.testLogin"                      # Single test method
cd apps/api && ./gradlew bootJar         # Build JAR only
```

## Monorepo Structure

```
apps/
  web/           # Next.js 15 frontend (App Router, React 19, TypeScript, Tailwind CSS 4, ShadCN/UI)
  api/           # Spring Boot 3.3 backend (Java 21, Gradle Kotlin DSL)
packages/
  eslint-config/
  typescript-config/
docs/            # Architecture, business rules, use cases, feature specs (Portuguese)
  features/      # Per-feature implementation specs (01-auth-profile.md through 07-premium.md)
  tasks.md       # Development tasks organized by milestone
```

## Architecture

### Frontend (`apps/web`)

- **Routing:** App Router with `[locale]` segment for i18n via next-intl. Locales: en, ja, de, fr, pt
- **Route groups:** `(auth)/` for login/register, `(main)/` for authenticated layouts
- **Rendering strategy:** SSG for landing, SSR for auth/public profiles (ISR 60s revalidation), CSR for dashboard/chat/calendar/matching
- **State management:**
  - Zustand stores (`auth-store`, `chat-store`, `notification-store`) for global client state
  - TanStack Query for server state with automatic cache invalidation
  - React Hook Form + Zod for typed form validation
  - nuqs for URL-persisted search/filter state
- **Auth flow:** JWT access token in memory (Zustand), refresh token in httpOnly cookie. Axios interceptors handle `Authorization: Bearer` injection and silent refresh on 401
- **WebSocket:** @stomp/stompjs + sockjs-client connecting to `/ws` with JWT as query param
- **i18n namespaces:** Separated by feature (`auth.login.title`, `static.create.name`). FFXIV-specific terms use `ffxiv.*` namespace with official localized terms
- **Key libraries:** Axios (HTTP), FullCalendar (calendar UI), Recharts (charts), Sonner (toasts), Lucide (icons), react-markdown

### Backend (`apps/api`)

- **Package by feature** under `com.ffxiv.planner.*` — each module (auth, profile, static_, matching, calendar, reputation, chat, premium, notification, integration) contains its own controller/service/repository/entity/dto/mapper/scheduler/event layers
- **The static module uses `static_/`** because `static` is a Java reserved word
- **API conventions:** Base path `/api/v1`, kebab-case URLs, camelCase JSON, UUID v4 IDs, RFC 7807 error responses via `GlobalExceptionHandler`, pagination as `?page=0&size=20&sort=createdAt,desc`
- **DTOs:** Java Records for request/response types
- **Entity mapping:** MapStruct (compile-time annotation processing, not reflection-based)
- **Auth:** JWT stateless with HS256 (jjwt library), 15min access tokens, 30-day refresh tokens with rotation. OAuth via Google/Discord
- **Authorization:** `@PreAuthorize` with SpEL expressions + custom `@RequiresPremium` annotation
- **Database:** PostgreSQL 16, Flyway migrations (V1–V11 in `src/main/resources/db/migration/`), UUID PKs, audit columns (`created_at`/`updated_at`) via `AuditableEntity` `@MappedSuperclass`, soft deletes via `deleted_at` on users/statics/reviews, JSONB for semi-structured data, enums stored as VARCHAR
- **Cross-module communication:** Spring `ApplicationEventPublisher` for domain events (not direct service-to-service calls)
- **Caching:** Spring Cache abstraction backed by Redis; write-through + TTL + event-driven invalidation
- **Rate limiting:** Redis sliding window by IP + endpoint
- **Scheduling:** Quartz jobs — matching recalc (daily 03:00 UTC), reminders (every 15min), application/invitation expiration (daily), subscription checks (daily), Lodestone data refresh (weekly), cleanup (weekly)
- **Profiles:** `application.yml` (shared), `application-dev.yml`, `application-prod.yml`
- **Uses Lombok** for boilerplate reduction

### Redis Key Patterns

| Pattern | TTL | Purpose |
|---------|-----|---------|
| `session:{userId}` | 30d | Refresh token hash for invalidation |
| `rate:{ip}:{endpoint}` | 1min | Sliding window rate limiter |
| `match:{characterId}` | 24h | Cached match scores |
| `profile:{characterId}` | 5min | Public profile cache |
| `count:matches:{userId}:{week}` | 7d | Free tier match limit (5/week) |
| `count:chats:{userId}` | — | Free tier DM limit (20 active) |
| `ws:presence:{userId}` | 5min | Online status heartbeat |
| `lock:matching:run` | 10min | Distributed lock for matching job |

### External Integrations

- **Lodestone/XIVAPI:** Character verification (bio code check) and data import via WebFlux reactive client
- **Stripe:** Subscription payments with webhook at `/api/v1/webhooks/stripe`
- **Google/Discord OAuth:** Alternative auth providers

### Public Endpoints (no auth required)

```
POST  /api/v1/auth/register, /login, /refresh, /forgot-password
GET   /api/v1/statics/{id}         (public profile)
GET   /api/v1/users/{id}/profile   (public profile)
GET   /api/v1/events/public        (PUG events)
POST  /api/v1/webhooks/stripe      (Stripe webhook)
```

## Key Business Rules

- **Freemium limits:** Free users: 1 static as leader, 5 matches/week, 20 active DMs. Premium: unlimited
- **Lodestone verification** required before matching/reputation features activate
- **Match scores** below 40% are hidden; matching prioritizes same Data Center
- **Reviews** require 2+ weeks of coexistence in a static
- **Expiration:** Applications expire after 14 days, invitations after 7 days
- **Account lockout** after 5 failed login attempts (15min lock)
- **Soft deletes** for users, statics, and reviews (never hard delete)

## Documentation Reference

All detailed docs in `docs/` (Portuguese):
- `architecture.md` — full system architecture, DB schema with all tables/indexes, complete API endpoint map, security config, Docker setup, Redis patterns, CI/CD pipeline
- `business-domain.md` — business rules RN01–RN45, domain model, personas, FFXIV glossary
- `use-cases.md` — detailed use cases UC01–UC28 with flows, exceptions, and acceptance criteria
- `features/01-07` — per-feature implementation specs with entity definitions, service contracts, controller endpoints, frontend components, and test plans
- `tasks.md` — granular development tasks organized by milestone (Milestone 0: Infra → Milestone 7: Premium)
