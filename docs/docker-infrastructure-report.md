# Infraestrutura Docker — Relatório de Implementação

## Status

- **docker compose config**: ✅ Sintaxe validada (dev e prod)
- **docker compose up** (dev): ✅ Todos os 4 serviços rodando
- **docker compose -f docker-compose.prod.yml up --build** (prod): ✅ Todos os 5 serviços rodando
- **API (Spring Boot)**: ✅ `Started FfxivPlannerApplication in 31.5s` — Tomcat na porta 8080
- **Web (Next.js)**: ✅ `Ready in 5.1s` — Turbopack na porta 3000
- **PostgreSQL**: ✅ Healthy na porta 5432 (interna)
- **Redis**: ✅ Healthy na porta 6379 (interna, com autenticação em prod)
- **Nginx** (prod): ✅ Reverse proxy na porta 8080 interna → host 80
- **HTTP localhost:80** (via Nginx): ✅ Proxy funcional para frontend e API
- **docker build sfp-web**: ✅ Build de produção concluído (Next.js 15.5.12, standalone output)

---

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

---

## Arquitetura Docker

### Desenvolvimento (`docker-compose.yml`)

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

### Produção (`docker-compose.prod.yml`)

Cinco serviços com Nginx reverse proxy como ponto de entrada único:

| Serviço | Build/Imagem | Porta | Notas |
|---------|-------------|-------|-------|
| `postgres` | postgres:16-alpine | interna 5432 | Não exposta ao host; acesso via `docker exec` |
| `redis` | redis:7-alpine | interna 6379 | Protegido com senha (`REDIS_PASSWORD`); não exposta ao host |
| `api` | `apps/api/Dockerfile` (JDK 21 → JRE 21, non-root) | interna 8080 | Acessível apenas via Nginx |
| `web` | `apps/web/Dockerfile` (deps → build → standalone, non-root) | interna 3000 | Acessível apenas via Nginx; filesystem read-only |
| `nginx` | `nginx/Dockerfile` (nginx:1.27-alpine, non-root) | **host 80** → 8080 | Ponto de entrada único; filesystem read-only |

Variáveis obrigatórias (`.env`):
- `POSTGRES_PASSWORD` — senha forte para PostgreSQL
- `JWT_SECRET` — mínimo 32 caracteres para HS256
- `REDIS_PASSWORD` — senha para autenticação Redis

---

## Nginx Reverse Proxy

### Arquivo: `nginx/nginx.conf`

Configuração baseada na spec de `docs/architecture.md` (seção 17.4), hardened para produção.

#### Roteamento

| Location | Upstream | Rate Limit | Body Limit | Notas |
|----------|----------|------------|------------|-------|
| `/` | `web:3000` | 10r/s (burst 20) | 1m (global) | Frontend Next.js |
| `/api/v1/auth/` | `api:8080` | 5r/s (burst 10) | 64k | Auth — rate limit mais restritivo |
| `/api/v1/webhooks/stripe` | `api:8080` | — (sem limite) | 1m | Stripe controla o fluxo |
| `/api/` | `api:8080` | 30r/s (burst 60) | 10m | API geral com upload |
| `/ws/` | `api:8080` | — | — | WebSocket (upgrade headers, timeout 24h) |
| `/actuator/` | `api:8080` | — | — | Restrito a redes Docker internas |

A ordem importa: Nginx usa "longest prefix match", então `/api/v1/auth/` e `/api/v1/webhooks/stripe` são avaliados antes de `/api/`.

#### Headers de segurança

| Header | Valor |
|--------|-------|
| `X-Frame-Options` | `SAMEORIGIN` |
| `X-Content-Type-Options` | `nosniff` |
| `X-XSS-Protection` | `0` (recomendação moderna — delega ao CSP) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `X-Permitted-Cross-Domain-Policies` | `none` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=(), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=()` |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' ws: wss:; frame-ancestors 'self'; base-uri 'self'; form-action 'self';` |

#### Proteções adicionais

- **`server_tokens off`** — oculta versão do Nginx em respostas e páginas de erro
- **User-Agent vazio** → silently drop (HTTP 444)
- **Scanners de vulnerabilidade** → drop (`.env`, `.git`, `wp-admin`, `phpinfo`, `phpmyadmin`)
- **Dotfiles** → `deny all; return 404` para qualquer `location ~ /\.`
- **Buffer limits** — `proxy_buffer_size 128k`, `large_client_header_buffers 4 16k` (previne ataques de overflow de proxy buffer)
- **Rate limiting retorna 429** — configurado via `limit_req_status 429` (em vez do padrão 503)

### Arquivo: `nginx/Dockerfile`

```dockerfile
FROM nginx:1.27-alpine
# Remove default configs e HTML
# Copia nginx.conf customizado
# USER nginx (non-root, porta não-privilegiada 8080)
# HEALTHCHECK via wget no localhost:8080
```

Hardening:
- **Non-root** — roda como usuário `nginx` (não requer `CAP_NET_BIND_SERVICE`)
- **Porta 8080** interna (não-privilegiada) — compose mapeia `80:8080`
- **Default config removido** — `default.conf` e `/usr/share/nginx/html/*` apagados
- **Healthcheck** — `wget -qO /dev/null http://localhost:8080/` a cada 30s

---

## Hardening do Docker Compose (Produção)

### Isolamento de rede

| Serviço | Antes | Depois |
|---------|-------|--------|
| `postgres` | `ports: "5432:5432"` (exposto ao host) | `expose: "5432"` (interno apenas) |
| `redis` | `ports: "6379:6379"` (exposto ao host, sem auth) | `expose: "6379"` + `--requirepass` |
| `api` | `ports: "8080:8080"` | `expose: "8080"` (já estava) |
| `web` | `ports: "3000:3000"` | `expose: "3000"` (já estava) |
| `nginx` | — (não existia) | `ports: "80:8080"` (único ponto de entrada) |

### Redis — autenticação e hardening

```yaml
command: >
  redis-server
  --requirepass ${REDIS_PASSWORD:?REDIS_PASSWORD is required}
  --maxmemory 256mb
  --maxmemory-policy allkeys-lru
  --rename-command FLUSHALL ""
  --rename-command FLUSHDB ""
  --rename-command DEBUG ""
  --rename-command CONFIG ""
```

Comandos perigosos (`FLUSHALL`, `FLUSHDB`, `DEBUG`, `CONFIG`) são desabilitados via `--rename-command ""`.

### Segurança de container

Aplicado a **todos** os 5 serviços:

| Medida | Valor | Finalidade |
|--------|-------|------------|
| `restart` | `unless-stopped` | Auto-recuperação de crashes |
| `security_opt` | `no-new-privileges:true` | Impede escalação de privilégios |
| `deploy.resources.limits` | CPU + memória por serviço | Previne DoS por esgotamento de recursos |
| `deploy.resources.reservations` | CPU + memória mínima | Garante recursos mínimos |

Limites de recursos por serviço:

| Serviço | CPU Limit | Memory Limit | CPU Reservation | Memory Reservation |
|---------|-----------|-------------|-----------------|-------------------|
| `postgres` | 1.0 | 512M | 0.25 | 256M |
| `redis` | 0.5 | 300M | 0.1 | 64M |
| `api` | 2.0 | 1G | 0.5 | 512M |
| `web` | 1.0 | 512M | 0.25 | 256M |
| `nginx` | 0.5 | 128M | 0.1 | 32M |

### Filesystem read-only

| Serviço | `read_only` | `tmpfs` |
|---------|-------------|---------|
| `web` | ✅ `true` | `/tmp` |
| `nginx` | ✅ `true` | `/tmp`, `/var/cache/nginx`, `/var/run` |

---

## Hardening do Spring Boot (Produção)

### `application.yml` (base)

| Configuração | Valor | Finalidade |
|-------------|-------|------------|
| `jackson.deserialization.fail-on-unknown-properties` | `true` | Rejeita campos JSON desconhecidos (previne mass-assignment) |
| `jpa.open-in-view` | `false` | Evita queries N+1 e lazy loading acidental |
| `jpa.hibernate.ddl-auto` | `validate` | Schema gerenciado exclusivamente pelo Flyway |

### `application-prod.yml`

| Configuração | Valor | Finalidade |
|-------------|-------|------------|
| `spring.data.redis.password` | `${REDIS_PASSWORD}` | Autenticação com Redis protegido |
| `server.forward-headers-strategy` | `native` | Confia nos headers do Nginx (`X-Forwarded-For`, `X-Forwarded-Proto`) |
| `server.error.include-stacktrace` | `never` | Não expõe stack traces ao cliente |
| `server.error.include-message` | `never` | Não expõe mensagens de exceção |
| `server.error.include-binding-errors` | `never` | Não expõe erros de validação internos |
| `server.error.include-exception` | `false` | Não expõe nome da classe de exceção |
| `management.endpoints.web.exposure.include` | `health,info,prometheus` | Apenas 3 actuator endpoints expostos |
| `management.endpoint.env.enabled` | `false` | Desabilita endpoint `/actuator/env` |
| `management.endpoint.beans.enabled` | `false` | Desabilita endpoint `/actuator/beans` |
| `management.endpoint.mappings.enabled` | `false` | Desabilita endpoint `/actuator/mappings` |
| `management.endpoint.health.show-details` | `when-authorized` | Detalhes de saúde apenas para autenticados |

---

## SSL/TLS (Opcional)

O HTTPS está preparado mas comentado no `nginx.conf`. Para ativar:

1. Descomentar o bloco `server { listen 8443 ssl http2; ... }` no `nginx.conf`
2. Montar certificados via volume no `docker-compose.prod.yml`:
   ```yaml
   volumes:
     - /etc/letsencrypt/live/yourdomain:/etc/nginx/certs:ro
   ```
3. Adicionar porta 443 no compose:
   ```yaml
   ports:
     - "80:8080"
     - "443:8443"
   ```
4. Substituir `ffxiv-planner.com` pelo domínio real

Configuração TLS pré-definida (Mozilla Modern):
- Protocolos: TLSv1.2, TLSv1.3
- Ciphers: ECDHE-ECDSA/RSA com AES128/256-GCM-SHA256/384
- OCSP Stapling ativo
- HSTS: `max-age=63072000; includeSubDomains; preload` (2 anos)
- Session tickets desabilitados

---

## Arquivos envolvidos

### Arquivos novos (criados nesta iteração)

| # | Arquivo | Descrição |
|---|---------|-----------|
| 1 | `nginx/nginx.conf` | Reverse proxy com rate limiting, security headers, CSP, proteção contra scanners |
| 2 | `nginx/Dockerfile` | Imagem hardened — non-root, sem configs default, healthcheck |

### Arquivos modificados (nesta iteração)

| # | Arquivo | Mudanças |
|---|---------|----------|
| 3 | `docker-compose.prod.yml` | +nginx service, postgres/redis internos, Redis auth, resource limits, restart policies, read-only, no-new-privileges |
| 4 | `apps/api/src/main/resources/application.yml` | `fail-on-unknown-properties: true` |
| 5 | `apps/api/src/main/resources/application-prod.yml` | +Redis password, +forward-headers-strategy, +error suppression, +actuator restrictions |
| 6 | `.env.example` | +`REDIS_PASSWORD`, notas de geração de secrets, URLs atualizadas |
| 7 | `CLAUDE.md` | Seção Production atualizada com 5 serviços, security hardening summary |

### Arquivos criados anteriormente (relatório original)

| # | Arquivo | Descrição |
|---|---------|-----------|
| 8 | `apps/api/pom.xml` | Dependências Maven (migração de Gradle) |
| 9 | `apps/api/.dockerignore` | Exclui `target/`, IDE, Docker |
| 10 | `apps/web/.dockerignore` | Exclui `.next/`, `node_modules/`, `.turbo/` |
| 11 | `apps/api/Dockerfile` | Multi-stage: JDK 21 build → JRE 21 runtime, non-root |
| 12 | `apps/web/Dockerfile` | Multi-stage: deps → build → standalone Node.js, non-root |
| 13 | `docker-compose.yml` | Dev: 4 serviços com hot reload |
| 14 | Maven Wrapper | `mvnw`, `.mvn/` — Maven Wrapper 3.3.4 |

---

## Verificação

### Desenvolvimento
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

### Produção
```
$ POSTGRES_PASSWORD=xxx JWT_SECRET=xxx REDIS_PASSWORD=xxx \
  docker compose -f docker-compose.prod.yml config
# ✅ Validação de sintaxe sem erros

$ docker compose -f docker-compose.prod.yml up --build -d
$ docker compose -f docker-compose.prod.yml ps
NAME           STATUS                    PORTS
sfp-api        Up (healthy)              8080/tcp
sfp-nginx      Up (healthy)              0.0.0.0:80->8080/tcp
sfp-postgres   Up (healthy)              5432/tcp
sfp-redis      Up (healthy)              6379/tcp
sfp-web        Up                        3000/tcp

# Rotas via Nginx:
$ curl -s -o /dev/null -w "%{http_code}" http://localhost/          # 200 (Next.js)
$ curl -s -o /dev/null -w "%{http_code}" http://localhost/api/v1/   # 401 (Spring Security)
$ curl -I http://localhost/ 2>/dev/null | grep -i "x-frame\|content-security\|server"
X-Frame-Options: SAMEORIGIN
Content-Security-Policy: default-src 'self'; ...
# Sem header "Server: nginx/x.x" (server_tokens off)
```

---

## Observações

- A **primeira execução** baixa Maven + dependências (~3 min) e npm packages (~2 min). Runs seguintes usam os volumes de cache (`maven-cache`, `root-node-modules`, `web-node-modules`).
- O **contexto de build do Dockerfile da API** é `apps/api/`.
- O **contexto de build do Dockerfile do Web** é a raiz do repositório (`.`) — precisa de `packages/` e workspace raiz.
- A **porta 5005** é para debug remoto da JVM (JDWP) — apenas em dev. Conecte a IDE em `localhost:5005`.
- Em produção, **todas as portas são internas** exceto Nginx (host 80 → container 8080).
- O **Redis em dev não tem senha** (funciona sem `.env`). Em produção, `REDIS_PASSWORD` é obrigatório.
- O **Nginx roda como non-root** (`USER nginx`) na porta não-privilegiada 8080 — o compose mapeia `80:8080` externamente.
