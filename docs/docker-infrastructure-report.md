# Infraestrutura Docker — Relatório de Implementação

## Status

- **docker compose config**: ✅ Sintaxe validada
- **docker compose up**: ✅ Todos os 4 serviços rodando
- **API (Spring Boot)**: ✅ `Started FfxivPlannerApplication in 31.5s` — Tomcat na porta 8080, devtools ativo
- **Web (Next.js)**: ✅ `Ready in 5.1s` — Turbopack na porta 3000
- **PostgreSQL**: ✅ Healthy na porta 5432
- **Redis**: ✅ Healthy na porta 6379
- **HTTP localhost:8080**: ✅ Responde 401 (Spring Security — esperado sem JWT)
- **HTTP localhost:3000**: ✅ Responde 200
- **docker build sfp-web**: ✅ Build de produção concluído (Next.js 15.5.12, standalone output)

## Migração Gradle → Maven

O sistema de build do backend foi migrado de Gradle (Kotlin DSL) para Maven durante a implementação. O Gradle apresentava problemas irrecuperáveis no Docker em ambiente Windows:

1. **CRLF no `gradlew`** — o Alpine Linux não conseguia executar o script com line endings Windows
2. **`sed -i` em bind mount NTFS** — edição in-place falha em filesystems montados do Windows
3. **FileHasher I/O error** — o Gradle daemon não conseguia criar hashes de arquivos em bind mount Windows
4. **JDWP + Gradle daemon** — `JAVA_TOOL_OPTIONS` afetava todos os processos Java, causando conflito de porta de debug

O Maven funciona sem nenhum desses problemas: o wrapper (`mvnw`) é convertido com `tr -d '\r'` para um arquivo local (`mvnw_lf`), roda como processo único via `spring-boot:run`, e o cache `.m2` é persistido em volume nomeado.

### Arquivos removidos (Gradle)
- `build.gradle.kts`, `settings.gradle.kts`
- `gradle/` (diretório do wrapper)
- `gradlew`, `gradlew.bat`

### Arquivos criados (Maven)
- `pom.xml` — todas as dependências migradas do `build.gradle.kts`
- `mvnw`, `mvnw.cmd` — Maven Wrapper 3.3.4
- `.mvn/wrapper/maven-wrapper.jar` e `maven-wrapper.properties`

## O que foi feito

### Arquivos novos (7)

#### 1. `apps/api/pom.xml`
Equivalente Maven do `build.gradle.kts` com todas as dependências:
- Spring Boot Starters (web, data-jpa, security, validation, websocket, data-redis, cache, mail, webflux, quartz)
- spring-boot-devtools (scope runtime, optional)
- PostgreSQL, Flyway
- jjwt 0.12.6
- MapStruct 1.6.3 com annotation processors (Lombok + Lombok-MapStruct binding + MapStruct processor)
- Stripe 28.3.0
- Lombok
- Testes: spring-boot-starter-test, spring-security-test, H2

#### 2. `apps/api/.dockerignore`
Exclui `target/`, arquivos de IDE e Docker do contexto de build.

#### 3. `apps/web/.dockerignore`
Exclui `.next/`, `node_modules/`, `.turbo/`, `coverage/`, arquivos de IDE e Docker do contexto de build.

#### 4. `apps/api/Dockerfile` (Produção)
Build multi-stage com Maven:
- **Stage 1 (build):** `eclipse-temurin:21-jdk-alpine` — corrige CRLF do `mvnw`, copia `pom.xml` primeiro (`dependency:go-offline` para cache de camadas), depois source, e executa `./mvnw package -DskipTests`.
- **Stage 2 (runtime):** `eclipse-temurin:21-jre-alpine` — copia apenas o JAR, roda como usuário não-root `appuser`, expõe porta 8080.

#### 5. `apps/web/Dockerfile` (Produção)
Build multi-stage com contexto na raiz do monorepo:
- **Stage 1 (deps):** `node:22-alpine` — copia `package.json` raiz e dos workspaces, `npm ci`.
- **Stage 2 (build):** Copia deps + source, `npm run build` (gera standalone).
- **Stage 3 (runtime):** `node:22-alpine` — copia `.next/standalone`, `.next/static`, `public/`, usuário não-root, porta 3000.

#### 6. `docker-compose.yml` (Desenvolvimento)
Quatro serviços com hot reload:

| Serviço | Imagem | Portas | Finalidade |
|---------|--------|--------|------------|
| `postgres` | postgres:16-alpine | 5432 | Banco com volume `pgdata`, healthcheck `pg_isready` |
| `redis` | redis:7-alpine | 6379 | Cache com volume `redisdata`, healthcheck `redis-cli ping` |
| `api` | eclipse-temurin:21-jdk-alpine | 8080, 5005 | Spring Boot dev + debug remoto via `spring-boot:run` com JDWP |
| `web` | node:22-alpine | 3000 | Next.js Turbopack com WATCHPACK_POLLING |

Decisões de design:
- **Sem Dockerfiles para dev** — containers usam imagens base com source bind-mounted.
- **CRLF handling** — `tr -d '\r' < mvnw > mvnw_lf` cria uma cópia limpa no mesmo diretório (necessário em Windows).
- **Debug JVM** — JDWP é passado via `-Dspring-boot.run.jvmArguments` (só afeta o processo Spring Boot, não o Maven).
- **Volumes nomeados** para `node_modules` (evita conflito Windows/Linux) e `.m2` (cache de dependências Maven).
- **depends_on com healthcheck** — API espera postgres e redis estarem saudáveis.
- **Defaults funcionais** — funciona sem `.env`.

#### 7. Maven Wrapper (`mvnw`, `.mvn/`)
Maven Wrapper 3.3.4 apontando para Maven 3.9.9, com JAR incluso (tipo `bin`).

### Arquivos modificados (4)

#### 8. `apps/web/next.config.ts`
Adicionado `output: "standalone"`. Gera `server.js` auto-contido (~150MB) no build. Não afeta `next dev`.

#### 9. `.env.example`
- Adicionados `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` para Docker.
- Default de `DATABASE_PASSWORD` alterado de vazio para `postgres`.
- Adicionados `NEXT_PUBLIC_API_URL` e `NEXT_PUBLIC_WS_URL`.
- Comentários sobre diferenças Docker vs bare-metal.

#### 10. `CLAUDE.md`
- Comandos atualizados de Gradle para Maven (`./mvnw spring-boot:run`, `./mvnw test`, etc.).
- Seção "Docker" adicionada com tabela de serviços, hot reload, monorepo handling.

#### 11. `apps/api/package.json`
Scripts Turborepo atualizados de `gradlew` para `mvnw`.

## Verificação executada

```
$ docker compose ps
NAME           STATUS                    PORTS
sfp-api        Up 2 minutes              0.0.0.0:5005->5005, 0.0.0.0:8080->8080
sfp-postgres   Up 2 minutes (healthy)    0.0.0.0:5432->5432
sfp-redis      Up 2 minutes (healthy)    0.0.0.0:6379->6379
sfp-web        Up 2 minutes              0.0.0.0:3000->3000

$ curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/
401

$ curl -s -o /dev/null -w "%{http_code}" http://localhost:3000
200
```

## Observações

- A **primeira execução** baixa Maven + dependências (~3 min) e npm packages (~2 min). Runs seguintes usam os volumes de cache (`maven-cache`, `root-node-modules`, `web-node-modules`).
- O **contexto de build do Dockerfile da API** é `apps/api/`.
- O **contexto de build do Dockerfile do Web** é a raiz do repositório (`.`) — precisa de `packages/` e workspace raiz.
- A **porta 5005** é para debug remoto da JVM (JDWP). Conecte a IDE em `localhost:5005`.
