# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FFXIV Static Planner (StaticForge) — a web platform for recruiting, forming, and managing raid statics in Final Fantasy XIV. Freemium model with Stripe-powered premium subscriptions.

**Stack**: Next.js 15 (App Router, React 19) + Spring Boot 3.3 (Java 21) + PostgreSQL 16 + Redis 7, orchestrated as a Turborepo monorepo with npm workspaces.

## Common Commands

### Development (full stack)
```bash
npm install                    # Install all workspace dependencies (run from root)
docker compose up -d           # Start PostgreSQL + Redis (dev infra only)
npm run dev                    # Start both frontend (:3000) and backend (:8080) via Turborepo
```

### Build & Lint
```bash
npm run build                  # Build all apps (turbo run build)
npm run lint                   # Lint all apps (turbo run lint)
npm run format                 # Format all files with Prettier
npm run format:check           # Check formatting without writing
```

### Backend (apps/api)
```bash
cd apps/api
./mvnw spring-boot:run                           # Run backend (dev profile, needs Docker infra)
./mvnw test                                       # Unit tests (H2 in-memory, no Docker needed)
./mvnw test -Dtest=ClassName                      # Run a single test class
./mvnw test -Dtest=ClassName#methodName           # Run a single test method
./mvnw verify -Dspring.profiles.active=ci         # Integration tests (needs real PostgreSQL + Redis)
./mvnw package -DskipTests                        # Build JAR without tests
```

### Frontend (apps/web)
```bash
cd apps/web
npm run dev                    # Next.js dev server with Turbopack on port 3000
npm run build                  # Production build (standalone output)
npm run lint                   # ESLint
```

### Turborepo Filtering
```bash
npx turbo run build --filter=@ffxiv-planner/web   # Build frontend only
npx turbo run lint --filter=@ffxiv-planner/web    # Lint frontend only
```

## Architecture

### Monorepo Layout
- **`apps/web`** — Next.js frontend (`@ffxiv-planner/web`)
- **`apps/api`** — Spring Boot backend (Maven, `com.ffxiv.planner`)
- **`packages/eslint-config`** — Shared ESLint config (`@ffxiv-planner/eslint-config`)
- **`packages/typescript-config`** — Shared TypeScript config (`@ffxiv-planner/typescript-config`)
- **`docs/`** — Detailed architecture, business domain, feature specs, and task tracking
- **`nginx/`** — Production reverse proxy config + Dockerfile

### Backend Architecture (Package-by-Feature)
Each domain feature under `com.ffxiv.planner` follows this structure:
```
feature/
├── controller/     # @RestController endpoints
├── service/        # @Service business logic
├── repository/     # Spring Data JPA repositories
├── entity/         # @Entity JPA entities
├── dto/            # Request/Response DTOs (Java Records)
├── mapper/         # MapStruct entity↔DTO mappers
├── scheduler/      # @Scheduled jobs (where applicable)
└── event/          # Domain events (where applicable)
```

**Feature packages**: `auth`, `profile`, `static_` (underscore because `static` is a Java reserved word), `matching`, `calendar`, `reputation`, `chat`, `premium`, `notification`, `integration` (lodestone, stripe), `common` (exceptions, shared DTOs, auditing).

**Key backend patterns**:
- Flyway for database migrations (`src/main/resources/db/migration/V*__*.sql`)
- MapStruct + Lombok for entity↔DTO conversion (annotation processor order matters in pom.xml: Lombok → lombok-mapstruct-binding → MapStruct)
- JWT authentication with access token (in-memory/Zustand) + refresh token (httpOnly cookie)
- Redis for caching (`@EnableCaching`), session pub/sub for WebSocket multi-instance, and rate limiting
- STOMP over SockJS for real-time features (chat, notifications, calendar updates)
- `server.servlet.context-path: /api/v1` — all REST endpoints are prefixed
- Jackson configured with `fail-on-unknown-properties: true` to prevent mass-assignment

### Frontend Architecture
- **App Router** with `[locale]` dynamic segment for i18n (planned: `next-intl`)
- **Route groups**: `(auth)` for login/register, `(main)` for authenticated pages
- **State management**: Zustand (global state), TanStack Query (server cache), React Hook Form + Zod (forms)
- **UI**: ShadCN/UI (Radix primitives) + Tailwind CSS 4
- **Standalone output**: `next.config.ts` sets `output: "standalone"` for Docker deployment

### Production Deployment
Nginx reverse proxy routes: `/` → Next.js, `/api/*` → Spring Boot, `/ws/*` → WebSocket. All services containerized via `docker-compose.prod.yml`. CI/CD via GitHub Actions with self-hosted runner for deployment.

## Spring Profiles

| Profile | Purpose | Database | Redis | Flyway |
|---------|---------|----------|-------|--------|
| `dev` (default) | Local development | PostgreSQL (Docker) | Redis (Docker) | Enabled |
| `test` | Unit tests (`./mvnw test`) | H2 in-memory | Disabled (simple cache) | Disabled |
| `ci` | CI integration tests | PostgreSQL (GH Actions service) | Redis (GH Actions service) | Disabled (uses ddl-auto: update) |
| `prod` | Production | PostgreSQL | Redis (with password) | Enabled |

## Environment Setup

Copy `.env.example` → `.env` and fill in values. The defaults work for local development with Docker Compose. Required for production: `JWT_SECRET` (min 32 chars), `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, OAuth client IDs/secrets, Stripe keys.

## Code Style

- **Prettier** handles formatting for both TS/JS and Java files. Java uses 4-space indentation; everything else uses 2-space.
- **ESLint**: shared config in `packages/eslint-config` (extends to `apps/web`)
- DTOs use Java Records; entities use Lombok annotations
- Backend follows Spring's layered conventions: Controller → Service → Repository

## CI Pipeline (PR Validation)

Three parallel jobs on every PR to `master`:
1. **Frontend**: `npm ci` → lint → build (filtered to `@ffxiv-planner/web`)
2. **Backend test**: tests with real PostgreSQL + Redis service containers → SonarCloud scan
3. **Backend build**: compile JAR (no tests)

All three must pass the gate job for PR approval.

## Domain Terminology

Documentation is written in Portuguese. Key FFXIV terms used throughout the codebase:
- **Static**: Fixed group of 8 players for raid content
- **Matching**: Automated system that pairs players with compatible statics based on role, schedule, data center, language, and progression
- **PUG (Pick-Up Group)**: One-time group events open to any player
- **Lodestone**: FFXIV's official player profile site (integrated via XIVAPI for character verification)
