# INFRA-01: Initialize Monorepo with Turborepo

> **Issue:** [#1 — INFRA-01: Initialize monorepo with Turborepo](https://github.com/mineirinhodev/StaticForge/issues/1)
> **Data de conclusão:** 2026-03-08
> **Referência:** `docs/architecture.md` seção 2

---

## Resumo

Configuração completa do monorepo FFXIV Static Planner utilizando **Turborepo 2.x** com **npm workspaces**, contendo o frontend Next.js 15, o backend Spring Boot 3.3 e pacotes compartilhados de configuração ESLint e TypeScript.

---

## Checklist de Entrega

- [x] Criar root `package.json` com workspaces
- [x] Criar `turbo.json` com pipelines (`dev`, `build`, `lint`, `test`)
- [x] Criar `apps/web/` (Next.js 15) com create-next-app
- [x] Criar `apps/api/` (Spring Boot 3.3) com Spring Initializr
- [x] Criar `packages/eslint-config/`
- [x] Criar `packages/typescript-config/`
- [x] Validar `npm run dev` rodando ambas as apps
- [x] Validar `npm run build` buildando tudo via Turborepo

---

## Estrutura Final do Monorepo

```
ffxiv-static-planner/
├── package.json              # Root — npm workspaces + scripts Turbo
├── package-lock.json         # Lockfile centralizado
├── turbo.json                # Pipelines do Turborepo
├── .gitignore                # Node, Java, IDE, Turbo, .env
├── CLAUDE.md                 # Instruções para IA
│
├── apps/
│   ├── web/                  # Frontend — Next.js 15
│   │   ├── package.json
│   │   ├── next.config.ts
│   │   ├── tsconfig.json
│   │   ├── postcss.config.mjs
│   │   ├── eslint.config.mjs
│   │   ├── public/
│   │   └── src/
│   │       └── app/
│   │           ├── layout.tsx
│   │           ├── page.tsx
│   │           └── globals.css
│   │
│   └── api/                  # Backend — Spring Boot 3.3
│       ├── package.json      # Wrapper npm para integração Turbo
│       ├── build.gradle.kts  # Gradle Kotlin DSL
│       ├── settings.gradle.kts
│       ├── gradlew / gradlew.bat
│       └── src/
│           ├── main/
│           │   ├── java/com/ffxiv/planner/
│           │   │   └── FfxivPlannerApplication.java
│           │   └── resources/
│           │       ├── application.yml
│           │       ├── application-dev.yml
│           │       ├── application-prod.yml
│           │       └── db/migration/
│           └── test/
│               └── java/com/ffxiv/planner/
│
├── packages/
│   ├── eslint-config/        # Configuração ESLint compartilhada
│   │   ├── package.json
│   │   ├── base.js
│   │   └── next.js
│   │
│   └── typescript-config/    # Configuração TypeScript compartilhada
│       ├── package.json
│       ├── base.json
│       └── nextjs.json
│
└── docs/                     # Documentação (Português)
    ├── architecture.md
    ├── business-domain.md
    ├── use-cases.md
    ├── tasks.md
    └── features/
```

---

## Passo a Passo Detalhado

### 1. Root `package.json` — Workspaces

Arquivo raiz que define o monorepo com npm workspaces e scripts que delegam ao Turborepo:

```json
{
  "name": "ffxiv-static-planner",
  "version": "0.1.0",
  "private": true,
  "workspaces": [
    "apps/*",
    "packages/*"
  ],
  "scripts": {
    "dev": "turbo run dev",
    "build": "turbo run build",
    "lint": "turbo run lint",
    "test": "turbo run test",
    "clean": "turbo run clean"
  },
  "devDependencies": {
    "turbo": "^2.4.4"
  },
  "engines": {
    "node": ">=20"
  },
  "packageManager": "npm@10.9.4"
}
```

**Detalhes:**
- `workspaces`: Registra `apps/*` e `packages/*` como pacotes do monorepo
- `scripts`: Cada comando delega ao `turbo run`, que executa em paralelo nos workspaces
- `engines`: Exige Node.js 20+ (LTS)
- Turbo 2.x instalado como devDependency na raiz

---

### 2. `turbo.json` — Pipelines

Configuração do Turborepo com 5 tasks e suas estratégias de cache:

```json
{
  "$schema": "https://turbo.build/schema.json",
  "globalDependencies": ["**/.env.*local"],
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": [".next/**", "!.next/cache/**", "build/**", "dist/**"]
    },
    "dev": {
      "cache": false,
      "persistent": true
    },
    "lint": {
      "dependsOn": ["^build"]
    },
    "test": {
      "dependsOn": ["^build"]
    },
    "clean": {
      "cache": false
    }
  }
}
```

**Detalhes das tasks:**

| Task | Cache | Comportamento |
|------|-------|---------------|
| `build` | Sim — outputs: `.next/**`, `build/**`, `dist/**` | Depende de `^build` (deps primeiro). Exclui `.next/cache/**` |
| `dev` | Não | `persistent: true` — mantém servidores rodando (não encerra) |
| `lint` | Sim | Depende de `^build` (precisa de tipos compilados) |
| `test` | Sim | Depende de `^build` |
| `clean` | Não | Remove artefatos, sem cache |

- `globalDependencies`: Arquivos `.env.*local` invalidam cache de todas as tasks
- `dependsOn: ["^build"]`: Garante que dependências do workspace são compiladas antes

---

### 3. `apps/web/` — Frontend Next.js 15

Criado com `create-next-app` e configurado para o monorepo.

#### `apps/web/package.json`

```json
{
  "name": "@ffxiv-planner/web",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "next dev --turbopack --port 3000",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "clean": "rm -rf .next node_modules"
  },
  "dependencies": {
    "next": "^15.1.0",
    "react": "^19.0.0",
    "react-dom": "^19.0.0"
  },
  "devDependencies": {
    "@ffxiv-planner/eslint-config": "*",
    "@ffxiv-planner/typescript-config": "*",
    "@types/node": "^22.0.0",
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "eslint": "^9.18.0",
    "eslint-config-next": "^15.1.0",
    "typescript": "^5.7.0",
    "tailwindcss": "^4.0.0",
    "@tailwindcss/postcss": "^4.0.0",
    "postcss": "^8.5.0"
  }
}
```

**Stack:**
- **Next.js 15.5.12** com App Router
- **React 19** + React DOM 19
- **Tailwind CSS 4** via `@tailwindcss/postcss`
- **TypeScript 5.7**
- **Turbopack** habilitado no dev (`--turbopack`)
- Consome configs compartilhados: `@ffxiv-planner/eslint-config` e `@ffxiv-planner/typescript-config`

#### `apps/web/next.config.ts`

```ts
import type { NextConfig } from "next";

const nextConfig: NextConfig = {};

export default nextConfig;
```

> **Nota:** A configuração original tinha `turbopack.root` apontando para a raiz do monorepo via `path.resolve(__dirname, "../../")`. Essa opção foi removida pois o Next.js 15.5+ infere automaticamente o workspace root a partir do lockfile, e a configuração manual causava um **Runtime Error no Turbopack** (`An unexpected Turbopack error occurred`).

#### `apps/web/postcss.config.mjs`

```js
const config = {
  plugins: {
    "@tailwindcss/postcss": {},
  },
};
export default config;
```

#### `apps/web/src/app/layout.tsx`

```tsx
import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "FFXIV Static Planner",
  description: "Find and manage your FFXIV raid static",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
```

#### `apps/web/src/app/page.tsx`

```tsx
export default function Home() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <main className="text-center">
        <h1 className="text-4xl font-bold mb-4">FFXIV Static Planner</h1>
        <p className="text-lg text-gray-600">
          Find and manage your FFXIV raid static
        </p>
      </main>
    </div>
  );
}
```

#### `apps/web/src/app/globals.css`

```css
@import "tailwindcss";

:root {
  --background: #ffffff;
  --foreground: #171717;
}

@media (prefers-color-scheme: dark) {
  :root {
    --background: #0a0a0a;
    --foreground: #ededed;
  }
}

body {
  color: var(--foreground);
  background: var(--background);
  font-family: Arial, Helvetica, sans-serif;
}
```

---

### 4. `apps/api/` — Backend Spring Boot 3.3

Criado com Spring Initializr e integrado ao Turborepo via `package.json` wrapper.

#### `apps/api/package.json` (Wrapper para Turbo)

```json
{
  "name": "@ffxiv-planner/api",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "gradlew bootRun",
    "build": "gradlew bootJar",
    "test": "gradlew test",
    "clean": "gradlew clean"
  }
}
```

> Este `package.json` não tem dependências Node — serve apenas como ponte para o Turborepo delegar os scripts Gradle.

#### `apps/api/build.gradle.kts`

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.3.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.ffxiv.planner"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // Stripe
    implementation("com.stripe:stripe-java:28.3.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

**Stack do Backend:**
- **Spring Boot 3.3.7** com Java 21
- **Gradle Kotlin DSL** com Spring Dependency Management
- **PostgreSQL 16** + Flyway migrations
- **Redis** para cache e rate limiting
- **Spring Security** + JWT (jjwt 0.12.6)
- **WebSocket** (STOMP) para chat em tempo real
- **WebFlux** para chamadas reativas (XIVAPI/Lodestone)
- **Quartz** para jobs agendados
- **MapStruct 1.6.3** + **Lombok** para mapeamento e boilerplate
- **Stripe 28.3.0** para pagamentos
- **H2** para testes (in-memory)

#### Application Class

```java
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class FfxivPlannerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FfxivPlannerApplication.class, args);
    }
}
```

---

### 5. `packages/eslint-config/` — Configuração ESLint Compartilhada

#### `packages/eslint-config/package.json`

```json
{
  "name": "@ffxiv-planner/eslint-config",
  "version": "0.0.0",
  "private": true,
  "license": "MIT",
  "files": ["base.js", "next.js"],
  "devDependencies": {
    "@eslint/js": "^9.18.0",
    "eslint-config-next": "^15.1.0",
    "eslint-config-prettier": "^10.0.1",
    "eslint-plugin-only-warn": "^1.1.0",
    "typescript-eslint": "^8.20.0"
  }
}
```

#### `base.js` — Regras base para qualquer pacote TS

```js
import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier";
import onlyWarn from "eslint-plugin-only-warn";
import tseslint from "typescript-eslint";

export default [
  js.configs.recommended,
  eslintConfigPrettier,
  ...tseslint.configs.recommended,
  { plugins: { onlyWarn } },
  { ignores: ["dist/**", "node_modules/**"] },
];
```

#### `next.js` — Regras específicas para Next.js (estende base)

```js
import js from "@eslint/js";
import eslintConfigPrettier from "eslint-config-prettier";
import onlyWarn from "eslint-plugin-only-warn";
import pluginNext from "@next/eslint-plugin-next";
import pluginReact from "eslint-plugin-react";
import pluginReactHooks from "eslint-plugin-react-hooks";
import tseslint from "typescript-eslint";

export default [
  js.configs.recommended,
  eslintConfigPrettier,
  ...tseslint.configs.recommended,
  { plugins: { onlyWarn } },
  {
    ...pluginReact.configs.flat.recommended,
    settings: { react: { version: "detect" } },
  },
  {
    plugins: { "@next/next": pluginNext },
    rules: {
      ...pluginNext.configs.recommended.rules,
      ...pluginNext.configs["core-web-vitals"].rules,
    },
  },
  {
    plugins: { "react-hooks": pluginReactHooks },
    rules: pluginReactHooks.configs.recommended.rules,
  },
  { rules: { "react/react-in-jsx-scope": "off" } },
  { ignores: [".next/**", "node_modules/**"] },
];
```

---

### 6. `packages/typescript-config/` — Configuração TypeScript Compartilhada

#### `packages/typescript-config/package.json`

```json
{
  "name": "@ffxiv-planner/typescript-config",
  "version": "0.0.0",
  "private": true,
  "license": "MIT",
  "files": ["base.json", "nextjs.json"]
}
```

#### `base.json` — Config base

```json
{
  "$schema": "https://json.schemastore.org/tsconfig",
  "display": "Default",
  "compilerOptions": {
    "composite": false,
    "declaration": true,
    "declarationMap": true,
    "esModuleInterop": true,
    "forceConsistentCasingInFileNames": true,
    "inlineSources": false,
    "isolatedModules": true,
    "moduleResolution": "bundler",
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "preserveWatchOutput": true,
    "skipLibCheck": true,
    "strict": true,
    "strictNullChecks": true
  },
  "exclude": ["node_modules"]
}
```

#### `nextjs.json` — Config Next.js (estende base)

```json
{
  "$schema": "https://json.schemastore.org/tsconfig",
  "display": "Next.js",
  "extends": "./base.json",
  "compilerOptions": {
    "target": "ES2017",
    "lib": ["dom", "dom.iterable", "esnext"],
    "allowJs": true,
    "noEmit": true,
    "module": "esnext",
    "resolveJsonModule": true,
    "jsx": "preserve",
    "incremental": true,
    "plugins": [{ "name": "next" }]
  }
}
```

**Uso no `apps/web/tsconfig.json`:**
```json
{
  "extends": "@ffxiv-planner/typescript-config/nextjs.json",
  "compilerOptions": {
    "paths": { "@/*": ["./src/*"] }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx", ".next/types/**/*.ts"],
  "exclude": ["node_modules"]
}
```

---

## Validação

### 7. `npm run dev` — Ambas as apps rodando

```bash
npm run dev
```

**Resultado:**
```
• turbo 2.8.14
• Packages in scope: @ffxiv-planner/api, @ffxiv-planner/eslint-config,
                     @ffxiv-planner/typescript-config, @ffxiv-planner/web
• Running dev in 4 packages

@ffxiv-planner/web:dev:  ▲ Next.js 15.5.12 (Turbopack)
@ffxiv-planner/web:dev:  - Local: http://localhost:3000
@ffxiv-planner/web:dev:  ✓ Ready in 2.7s

@ffxiv-planner/api:dev:  :: Spring Boot :: (v3.3.7)
@ffxiv-planner/api:dev:  Started FfxivPlannerApplication in 6.976 seconds
@ffxiv-planner/api:dev:  Tomcat started on port 8080 with context path '/api/v1'
```

| App | URL | Tempo de startup |
|-----|-----|-----------------|
| Frontend (Next.js) | `http://localhost:3000` | ~2.7s |
| Backend (Spring Boot) | `http://localhost:8080/api/v1` | ~7.0s |

### 8. `npm run build` — Build completo

```bash
npm run build
```

**Resultado:**
```
• turbo 2.8.14
• Running build in 4 packages

@ffxiv-planner/web:build:  ▲ Next.js 15.5.12
@ffxiv-planner/web:build:  ✓ Compiled successfully in 7.5s
@ffxiv-planner/web:build:  ✓ Generating static pages (4/4)
@ffxiv-planner/web:build:  Route (app)              Size    First Load JS
@ffxiv-planner/web:build:  ┌ ○ /                    122 B   102 kB
@ffxiv-planner/web:build:  └ ○ /_not-found          985 B   103 kB

@ffxiv-planner/api:build:  BUILD SUCCESSFUL in 25s

Tasks:    2 successful, 2 total
Cached:   1 cached, 2 total
Time:     27.47s
```

| App | Artefato | Tamanho |
|-----|----------|---------|
| Frontend | `.next/` (static pages) | First Load ~102kB |
| Backend | `build/libs/*.jar` | Spring Boot fat JAR |

---

## Bug Fix Aplicado

### Turbopack Runtime Error

**Problema:** Ao acessar `http://localhost:3000`, aparecia:
```
Next.js 15.5.12
Runtime Error — An unexpected Turbopack error occurred.
```

**Causa:** O `next.config.ts` original configurava `turbopack.root` manualmente:
```ts
// ANTES (com bug)
const nextConfig: NextConfig = {
  turbopack: {
    root: path.resolve(__dirname, "../../"),
  },
};
```

O Next.js 15.5+ infere automaticamente o workspace root a partir do lockfile (`package-lock.json`). A configuração manual conflitava com essa detecção automática.

**Correção:** Removida a propriedade `turbopack.root`:
```ts
// DEPOIS (corrigido)
const nextConfig: NextConfig = {};
```

---

## Pré-requisitos para Rodar

| Dependência | Versão | Necessário para |
|-------------|--------|-----------------|
| Node.js | >= 20 (LTS) | Frontend + Turborepo |
| npm | >= 10 | Workspaces |
| Java | 21 (Temurin/Adoptium) | Backend |
| PostgreSQL | 16 | Banco de dados |
| Redis | Latest | Cache + rate limiting |
| Docker | Latest | PostgreSQL + Redis (opcional) |

### Comandos para subir a infra:

```bash
# Subir PostgreSQL + Redis via Docker
docker compose up postgres redis

# Instalar dependências Node
npm install

# Rodar tudo em dev
npm run dev

# Build de produção
npm run build
```

---

## Comandos Úteis

```bash
# Desenvolvimento
npm run dev              # Todas as apps via Turborepo
npm run build            # Build de todas as apps
npm run lint             # Lint de todos os pacotes
npm run test             # Testes de todos os pacotes
npm run clean            # Limpa artefatos de build

# Frontend isolado
cd apps/web && npm run dev    # Dev server na :3000
cd apps/web && npm run build  # Build de produção

# Backend isolado
cd apps/api && ./gradlew bootRun   # Dev server na :8080
cd apps/api && ./gradlew test      # Testes
cd apps/api && ./gradlew bootJar   # Build JAR
```
