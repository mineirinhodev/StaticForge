# Feature 05 - Reputacao

## 1. Visao Geral

O modulo de Reputacao implementa o sistema de avaliacoes bidirecionais entre jogadores e statics. Permite que membros avaliem a static e que a static (via lider) avalie membros, criando um historico publico de reputacao. O sistema inclui 5 criterios de avaliacao, calculo de media ponderada com peso temporal, e moderacao de avaliacoes abusivas.

### 1.1 Rastreabilidade

| Tipo | IDs |
|------|-----|
| **Regras de Negocio** | RN30, RN31, RN32, RN33, RN34, RN35, RN36 |
| **Casos de Uso** | UC20 (Avaliar Jogador/Static), UC21 (Ver Reputacao), UC22 (Reportar Avaliacao) |

---

## 2. Backend

### 2.1 Entities (JPA)

#### Review

```java
@Entity
@Table(name = "reviews")
public class Review extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Character reviewer; // quem avaliou

    @Column(name = "target_id", nullable = false)
    private UUID targetId; // ID do character ou static avaliado

    @Column(name = "target_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReviewTargetType targetType; // PLAYER ou STATIC

    @Column(name = "static_id")
    private UUID staticId; // static onde a convivencia ocorreu

    // Criterios de avaliacao (1-5 estrelas cada)
    @Column(nullable = false)
    @Min(1) @Max(5)
    private int punctuality; // pontualidade

    @Column(nullable = false)
    @Min(1) @Max(5)
    private int communication; // comunicacao

    @Column(nullable = false)
    @Min(1) @Max(5)
    private int performance; // performance em combate

    @Column(nullable = false)
    @Min(1) @Max(5)
    private int attitude; // atitude e comportamento

    @Column(nullable = false)
    @Min(1) @Max(5)
    private int commitment; // comprometimento

    @Column(length = 2000)
    private String comment; // comentario textual opcional

    @Column(name = "deleted_at")
    private Instant deletedAt; // soft delete (por moderacao)

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL)
    private List<ReviewReport> reports = new ArrayList<>();
}

public enum ReviewTargetType {
    PLAYER,  // avaliacao de um jogador
    STATIC   // avaliacao de uma static
}
```

#### ReviewReport

```java
@Entity
@Table(name = "review_reports")
public class ReviewReport extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ReportReason reason; // HARASSMENT, FALSE_INFO, SPAM, INAPPROPRIATE, OTHER

    @Column(length = 1000)
    private String details;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReportStatus status; // PENDING, REVIEWED, UPHELD, DISMISSED

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy; // admin user ID

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;
}

public enum ReportReason {
    HARASSMENT,
    FALSE_INFO,
    SPAM,
    INAPPROPRIATE,
    OTHER
}

public enum ReportStatus {
    PENDING,
    REVIEWED,
    UPHELD,     // report valido - review removida
    DISMISSED   // report invalido
}
```

### 2.2 DTOs (Java Records)

#### Request DTOs

```java
// Criar/Editar avaliacao
public record CreateReviewRequest(
    @NotNull
    UUID targetId,

    @NotNull
    ReviewTargetType targetType,

    UUID staticId, // static onde a convivencia ocorreu

    @Min(1) @Max(5)
    int punctuality,

    @Min(1) @Max(5)
    int communication,

    @Min(1) @Max(5)
    int performance,

    @Min(1) @Max(5)
    int attitude,

    @Min(1) @Max(5)
    int commitment,

    @Size(max = 2000)
    String comment
) {}

// Reportar avaliacao
public record CreateReportRequest(
    @NotNull
    ReportReason reason,

    @Size(max = 1000)
    String details
) {}
```

#### Response DTOs

```java
// Avaliacao
public record ReviewResponse(
    UUID id,
    CharacterSummaryResponse reviewer,
    UUID targetId,
    ReviewTargetType targetType,
    UUID staticId,
    String staticName,
    int punctuality,
    int communication,
    int performance,
    int attitude,
    int commitment,
    double averageScore,
    String comment,
    Instant createdAt
) {}

// Reputacao agregada
public record ReputationResponse(
    UUID targetId,
    ReviewTargetType targetType,
    double overallScore,          // media ponderada geral (0-5)
    int totalReviews,
    ReputationBreakdown breakdown,
    List<ReviewResponse> recentReviews, // ultimas 5
    ReputationTrend trend               // tendencia (melhorando/estavel/piorando)
) {}

public record ReputationBreakdown(
    double punctuality,
    double communication,
    double performance,
    double attitude,
    double commitment
) {}

public record ReputationTrend(
    String direction,    // IMPROVING, STABLE, DECLINING
    double change        // variacao no ultimo mes
) {}
```

#### Mapper (MapStruct)

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CharacterMapper.class})
public interface ReviewMapper {
    ReviewResponse toResponse(Review review);

    @Mapping(target = "averageScore",
             expression = "java((review.getPunctuality() + review.getCommunication() + " +
                          "review.getPerformance() + review.getAttitude() + " +
                          "review.getCommitment()) / 5.0)")
    ReviewResponse toResponseWithAverage(Review review);
}
```

### 2.3 Repository

```java
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("SELECT r FROM Review r WHERE r.targetId = :targetId AND r.targetType = :type " +
           "AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findByTarget(@Param("targetId") UUID targetId,
                              @Param("type") ReviewTargetType type,
                              Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.targetId = :targetId AND r.targetType = :type " +
           "AND r.deletedAt IS NULL")
    List<Review> findAllByTarget(@Param("targetId") UUID targetId,
                                 @Param("type") ReviewTargetType type);

    boolean existsByReviewerIdAndTargetIdAndTargetType(UUID reviewerId, UUID targetId, ReviewTargetType type);

    @Query("SELECT AVG((r.punctuality + r.communication + r.performance + r.attitude + r.commitment) / 5.0) " +
           "FROM Review r WHERE r.targetId = :targetId AND r.targetType = :type AND r.deletedAt IS NULL")
    Optional<Double> calculateAverageScore(@Param("targetId") UUID targetId,
                                            @Param("type") ReviewTargetType type);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.targetId = :targetId AND r.targetType = :type AND r.deletedAt IS NULL")
    int countByTarget(@Param("targetId") UUID targetId, @Param("type") ReviewTargetType type);

    @Query("SELECT r FROM Review r WHERE r.reviewer.id = :reviewerId AND r.deletedAt IS NULL")
    List<Review> findByReviewerId(@Param("reviewerId") UUID reviewerId);
}

public interface ReviewReportRepository extends JpaRepository<ReviewReport, UUID> {

    List<ReviewReport> findByReviewIdAndStatus(UUID reviewId, ReportStatus status);

    @Query("SELECT COUNT(r) FROM ReviewReport r WHERE r.review.id = :reviewId")
    int countByReviewId(@Param("reviewId") UUID reviewId);

    boolean existsByReviewIdAndReporterId(UUID reviewId, UUID reporterId);
}
```

### 2.4 Service

#### ReviewService

```java
@Service
@Transactional
public class ReviewService {

    /**
     * Criar avaliacao.
     * Fluxo:
     * 1. Verifica que o reviewer tem character verificado
     * 2. Verifica que reviewer e target compartilharam a mesma static
     * 3. Valida tempo minimo de convivencia: 2 semanas (RN32)
     *    - Busca memberships do reviewer e target na mesma static
     *    - Calcula sobreposicao de tempo (ambos ativos na static por >= 14 dias)
     * 4. Verifica que nao existe review duplicada (mesmo reviewer->target)
     * 5. Verifica que reviewer nao esta avaliando a si mesmo
     * 6. Cria Review com os 5 criterios (RN33)
     * 7. Invalida cache de reputacao do target
     * 8. Publica ReviewCreatedEvent
     * 9. Verifica se a reputacao do target caiu abaixo de 2.0 (RN36)
     *
     * Regras: RN30 (bidirecional), RN31 (publica), RN32 (2 semanas), RN33 (5 criterios)
     */
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request);

    /**
     * Editar avaliacao.
     * Apenas o autor pode editar. Verifica ownership.
     */
    public ReviewResponse updateReview(UUID userId, UUID reviewId, CreateReviewRequest request);

    /**
     * Listar avaliacoes de um jogador.
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getPlayerReviews(UUID characterId, Pageable pageable);

    /**
     * Listar avaliacoes de uma static.
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getStaticReviews(UUID staticId, Pageable pageable);
}
```

#### ReputationService

```java
@Service
@Transactional(readOnly = true)
public class ReputationService {

    /**
     * Obter reputacao agregada de um jogador ou static.
     * Fluxo:
     * 1. Busca todas as reviews do target
     * 2. Calcula media ponderada por criterio
     *    - Peso temporal: reviews mais recentes tem peso maior (RN35)
     *    - Formula: peso = 1.0 / (1 + diasDesdeReview / 180)
     *    - Normaliza pesos para somar 1.0
     * 3. Calcula score geral (media dos 5 criterios ponderados)
     * 4. Calcula tendencia (comparando media do ultimo mes vs anterior)
     * 5. Retorna ReputationResponse com breakdown e ultimas reviews
     *
     * Regras: RN35 (media ponderada temporal)
     */
    @Cacheable(value = "reputation", key = "#targetId + ':' + #targetType")
    public ReputationResponse getReputation(UUID targetId, ReviewTargetType targetType);

    /**
     * Verificar se a reputacao esta abaixo do threshold.
     * Retorna true se score < 2.0 (RN36).
     * Usado internamente pelo MatchingService para restricao.
     */
    public boolean isBelowThreshold(UUID targetId, ReviewTargetType targetType);
}
```

#### Validacao Customizada: @MinCoexistenceWeeks

```java
// Annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MinCoexistenceWeeks {
    int value() default 2;
}

// Validator (usado internamente no ReviewService)
@Component
public class CoexistenceValidator {

    /**
     * Valida que reviewer e target compartilharam uma static por pelo menos N semanas.
     *
     * Logica:
     * 1. Busca todas as memberships do reviewer e target
     * 2. Filtra memberships na mesma static (staticId match)
     * 3. Para cada par, calcula sobreposicao temporal:
     *    - overlapStart = max(reviewer.joinedAt, target.joinedAt)
     *    - overlapEnd = min(reviewer.leftAt ?? now, target.leftAt ?? now)
     *    - overlapDays = (overlapEnd - overlapStart).toDays()
     * 4. Se alguma sobreposicao >= minWeeks * 7 dias, validacao passa
     * 5. Senao, lanca InsufficientCoexistenceException
     */
    public void validate(UUID reviewerId, UUID targetId, int minWeeks);
}
```

#### Moderacao de Reports

```java
@Service
@Transactional
public class ReviewReportService {

    /**
     * Reportar avaliacao.
     * Fluxo:
     * 1. Verifica que o reporter nao e o autor da review
     * 2. Verifica que o reporter nao ja reportou esta review
     * 3. Cria ReviewReport com status PENDING
     * 4. Se acumular 3+ reports, marca review para revisao prioritaria
     * 5. Publica ReviewReportedEvent (para admins)
     *
     * Regras: RN34 (reviews podem ser reportadas)
     */
    public void reportReview(UUID userId, UUID reviewId, CreateReportRequest request);

    // Metodos de admin (futuro)
    // public void resolveReport(UUID adminId, UUID reportId, ReportStatus resolution, String note);
}
```

#### Eventos Publicados

| Evento | Publicado por | Dados | Consumido por |
|--------|--------------|-------|---------------|
| `ReviewCreatedEvent` | `ReviewService.createReview` | reviewId, targetId, targetType, score | `NotificationService` (notifica avaliado), `ReputationService` (invalida cache) |
| `ReviewReportedEvent` | `ReviewReportService.reportReview` | reportId, reviewId | `NotificationService` (alerta admins) |
| `ReputationBelowThresholdEvent` | `ReviewService.createReview` | targetId, score | `MatchingService` (restricao - RN36), `NotificationService` (alerta usuario) |

#### Eventos Consumidos

| Evento | Origem | Acao |
|--------|--------|------|
| `MemberJoinedEvent` | `StaticService` | Registra inicio de coexistencia |
| `MemberLeftEvent` | `StaticService` | Registra fim de coexistencia |

### 2.5 Controller

```
@RestController
@RequestMapping("/api/v1")
```

| Metodo | Path | Descricao | Auth | Status |
|--------|------|-----------|------|--------|
| POST | `/reviews` | Criar avaliacao | Sim | 201 |
| PUT | `/reviews/{id}` | Editar avaliacao | Sim (Author) | 200 |
| GET | `/users/{id}/reviews` | Avaliacoes de um jogador | Nao | 200 |
| GET | `/statics/{id}/reviews` | Avaliacoes de uma static | Nao | 200 |
| GET | `/users/{id}/reputation` | Reputacao agregada | Nao | 200 |
| POST | `/reviews/{id}/report` | Reportar avaliacao | Sim | 201 |

**Exemplos de Request/Response:**

```json
// POST /api/v1/reviews
// Request:
{
  "targetId": "660e8400-e29b-41d4-a716-446655440001",
  "targetType": "PLAYER",
  "staticId": "880e8400-e29b-41d4-a716-446655440020",
  "punctuality": 5,
  "communication": 4,
  "performance": 4,
  "attitude": 5,
  "commitment": 5,
  "comment": "Excellent healer, always on time and communicates well. Great attitude."
}

// Response (201):
{
  "id": "dd0e8400-e29b-41d4-a716-446655440050",
  "reviewer": {
    "id": "770e8400-...",
    "name": "Marcus Shield",
    "server": "Ragnarok",
    "dataCenter": "Chaos",
    "isVerified": true,
    "avatarUrl": "https://...",
    "mainJobName": "PLD"
  },
  "targetId": "660e8400-...",
  "targetType": "PLAYER",
  "staticId": "880e8400-...",
  "staticName": "Heavens' Gate",
  "punctuality": 5,
  "communication": 4,
  "performance": 4,
  "attitude": 5,
  "commitment": 5,
  "averageScore": 4.6,
  "comment": "Excellent healer, always on time and communicates well. Great attitude.",
  "createdAt": "2026-03-04T22:00:00Z"
}
```

```json
// GET /api/v1/users/{id}/reputation
// Response (200):
{
  "targetId": "660e8400-...",
  "targetType": "PLAYER",
  "overallScore": 4.3,
  "totalReviews": 12,
  "breakdown": {
    "punctuality": 4.5,
    "communication": 4.2,
    "performance": 4.1,
    "attitude": 4.6,
    "commitment": 4.1
  },
  "recentReviews": [ ... ],
  "trend": {
    "direction": "IMPROVING",
    "change": 0.3
  }
}
```

### 2.6 Seguranca e Autorizacao

```java
// Avaliacoes podem ser lidas por qualquer um (RN31 - publicas)
.requestMatchers(HttpMethod.GET, "/api/v1/users/*/reviews").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/statics/*/reviews").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/users/*/reputation").permitAll()

// Criar e editar requer autenticacao
// Editar requer ser o autor (@PreAuthorize ou verificacao no service)
```

### 2.7 Validacoes

| Campo | Regra | Contexto |
|-------|-------|----------|
| `punctuality/communication/performance/attitude/commitment` | Inteiro entre 1 e 5 | CreateReviewRequest (RN33) |
| `comment` | Max 2000 chars | CreateReviewRequest |
| Coexistencia | Minimo 2 semanas na mesma static (RN32) | ReviewService |
| Auto-avaliacao | Reviewer nao pode avaliar a si mesmo | ReviewService |
| Duplicata | Uma avaliacao por par reviewer-target-type | ReviewService |
| `reason` (report) | Enum valido | CreateReportRequest |
| Report duplicado | Um report por usuario por review | ReviewReportService |

### 2.8 Cache (Redis)

| Chave | Tipo | TTL | Descricao | Invalidacao |
|-------|------|-----|-----------|-------------|
| `reputation:{targetId}:{targetType}` | String (JSON) | 10 min | Cache da reputacao agregada | Nova review criada/editada |
| `reviews:{targetId}:{targetType}:page:{n}` | String (JSON) | 5 min | Cache paginado de reviews | Nova review |

### 2.9 Jobs Agendados

Nao ha jobs especificos para este modulo. A verificacao de reputacao abaixo do threshold (RN36) e feita de forma reativa, no momento da criacao de cada review.

---

## 3. Frontend

### 3.1 Paginas e Rotas

| Rota | Renderizacao | Descricao |
|------|-------------|-----------|
| `/[locale]/users/[id]` | SSR + ISR (60s) | Perfil publico (inclui tab de reputacao) |
| `/[locale]/(main)/statics/[id]` | SSR + ISR (60s) | Pagina da static (inclui tab de avaliacoes) |

A reputacao e integrada nas paginas de perfil e static como tabs/secoes, sem paginas dedicadas.

### 3.2 Componentes React

```
components/reputation/
  ReputationCard                    -- Card resumido da reputacao
    OverallScore                    -- Score circular (4.3/5.0)
    StarRating                      -- Estrelas visuais
    ReviewCount                     -- Total de avaliacoes
    TrendIndicator                  -- Seta de tendencia (melhorando/piorando)
  ReputationChart                   -- Grafico radar (Recharts)
    RadarChart                      -- 5 eixos (pontualidade, comunicacao, etc.)
  ReviewList                        -- Lista de avaliacoes
    ReviewCard                      -- Card de avaliacao individual
      ReviewerInfo                  -- Info do avaliador (avatar, nome, static)
      CriteriaScores                -- Scores por criterio com barras
      ReviewComment                 -- Comentario textual
      ReviewDate                    -- Data da avaliacao
      ReportButton                  -- Botao de report
  ReviewForm                        -- Formulario de avaliacao
    CriteriaSliders                 -- 5 sliders (1-5 estrelas)
      StarSlider                    -- Slider individual com estrelas
    CommentInput                    -- Textarea para comentario
    SubmitButton
  ReportDialog                      -- Dialog de report
    ReasonSelect                    -- Select de motivo
    DetailsInput                    -- Campo de detalhes
  ReputationSummary                 -- Resumo compacto (para uso em cards)
    MiniStars                       -- Estrelas em miniatura
    ScoreLabel                      -- Score numerico
```

### 3.3 Estado e Data Fetching

#### Hooks TanStack Query

```typescript
// hooks/use-reputation.ts

function usePlayerReviews(characterId: string, page: number = 0) {
  return useQuery({
    queryKey: ['reviews', 'player', characterId, page],
    queryFn: () => api.get(`/users/${characterId}/reviews?page=${page}&size=10`),
  });
}

function useStaticReviews(staticId: string, page: number = 0) {
  return useQuery({
    queryKey: ['reviews', 'static', staticId, page],
    queryFn: () => api.get(`/statics/${staticId}/reviews?page=${page}&size=10`),
  });
}

function useReputation(userId: string) {
  return useQuery({
    queryKey: ['reputation', userId],
    queryFn: () => api.get(`/users/${userId}/reputation`),
    staleTime: 60_000,
  });
}

function useCreateReview() {
  return useMutation({
    mutationFn: (data: CreateReviewRequest) => api.post('/reviews', data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['reviews', variables.targetType.toLowerCase(), variables.targetId],
      });
      queryClient.invalidateQueries({
        queryKey: ['reputation', variables.targetId],
      });
    },
  });
}

function useUpdateReview() {
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateReviewRequest }) =>
      api.put(`/reviews/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      queryClient.invalidateQueries({ queryKey: ['reputation'] });
    },
  });
}

function useReportReview() {
  return useMutation({
    mutationFn: ({ reviewId, data }: { reviewId: string; data: CreateReportRequest }) =>
      api.post(`/reviews/${reviewId}/report`, data),
  });
}
```

### 3.4 Formularios e Validacao (Zod)

```typescript
// schemas/review.ts
import { z } from 'zod';

export const createReviewSchema = z.object({
  targetId: z.string().uuid(),
  targetType: z.enum(['PLAYER', 'STATIC']),
  staticId: z.string().uuid().optional(),
  punctuality: z.number().int().min(1).max(5),
  communication: z.number().int().min(1).max(5),
  performance: z.number().int().min(1).max(5),
  attitude: z.number().int().min(1).max(5),
  commitment: z.number().int().min(1).max(5),
  comment: z.string().max(2000).optional(),
});

export const createReportSchema = z.object({
  reason: z.enum(['HARASSMENT', 'FALSE_INFO', 'SPAM', 'INAPPROPRIATE', 'OTHER']),
  details: z.string().max(1000).optional(),
});
```

### 3.5 WebSocket

Nao aplicavel diretamente. Notificacoes de novas avaliacoes sao enviadas via `/user/queue/notifications`.

### 3.6 Internacionalizacao

**Namespaces i18n:**

| Namespace | Uso |
|-----------|-----|
| `reputation.*` | Componentes de reputacao |
| `reputation.criteria.*` | Nomes dos criterios (Punctuality, Communication, etc.) |
| `reputation.trend.*` | Labels de tendencia |
| `review.*` | Formulario e lista de avaliacoes |
| `review.report.*` | Dialog de report |
| `review.report.reasons.*` | Motivos de report |

---

## 4. Banco de Dados

### 4.1 DDL

```sql
-- Migration: V6__create_reviews_reputation.sql

CREATE TABLE reviews (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reviewer_id     UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    target_id       UUID NOT NULL,
    target_type     VARCHAR(20) NOT NULL, -- PLAYER, STATIC
    static_id       UUID REFERENCES statics(id),
    punctuality     INTEGER NOT NULL CHECK (punctuality BETWEEN 1 AND 5),
    communication   INTEGER NOT NULL CHECK (communication BETWEEN 1 AND 5),
    performance     INTEGER NOT NULL CHECK (performance BETWEEN 1 AND 5),
    attitude        INTEGER NOT NULL CHECK (attitude BETWEEN 1 AND 5),
    commitment      INTEGER NOT NULL CHECK (commitment BETWEEN 1 AND 5),
    comment         VARCHAR(2000),
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_review_unique UNIQUE (reviewer_id, target_id, target_type)
);

CREATE TABLE review_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id       UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    reporter_id     UUID NOT NULL REFERENCES users(id),
    reason          VARCHAR(50) NOT NULL,
    details         VARCHAR(1000),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_at     TIMESTAMPTZ,
    resolved_by     UUID REFERENCES users(id),
    resolution_note VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_report_unique UNIQUE (review_id, reporter_id)
);

-- Indices
CREATE INDEX idx_reviews_target ON reviews(target_id, target_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_reviewer ON reviews(reviewer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_static ON reviews(static_id) WHERE static_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_review_reports_review ON review_reports(review_id);
CREATE INDEX idx_review_reports_status ON review_reports(status) WHERE status = 'PENDING';
```

### 4.2 Indices

| Tabela | Indice | Tipo | Justificativa |
|--------|--------|------|---------------|
| `reviews` | `target_id, target_type` | BTREE (partial) | Reputacao do alvo |
| `reviews` | `reviewer_id` | BTREE (partial) | Reviews feitas por um jogador |
| `reviews` | `static_id` | BTREE (partial) | Reviews de uma static |
| `reviews` | `created_at` | BTREE DESC (partial) | Ordenacao cronologica |
| `reviews` | `reviewer_id, target_id, target_type` | UNIQUE | Uma review por par |
| `review_reports` | `review_id` | BTREE | Reports de uma review |
| `review_reports` | `status` | BTREE (partial) | Reports pendentes |
| `review_reports` | `review_id, reporter_id` | UNIQUE | Um report por usuario por review |

---

## 5. Integracao Entre Modulos

### 5.1 Dependencias

```
reputation
    |
    +--> auth-profile (Character para reviewer e target)
    |
    +--> static-management (Static para contexto de convivencia, memberships para validacao)
    |
    +--> notification (notifica sobre novas avaliacoes e reports)
    |
    <-- matching (consulta reputacao para filtrar matches com score < 2.0 - RN36)
    |
    <-- static-management (MemberJoinedEvent/MemberLeftEvent para rastrear coexistencia)
```

---

## 6. Tratamento de Erros

| HTTP | Tipo RFC 7807 | Cenario |
|------|---------------|---------|
| 400 | `/errors/validation` | Campos invalidos |
| 400 | `/errors/invalid-score` | Score fora de 1-5 |
| 403 | `/errors/character-not-verified` | Character nao verificado |
| 403 | `/errors/not-review-author` | Nao e o autor da review |
| 404 | `/errors/review-not-found` | Review nao encontrada |
| 404 | `/errors/target-not-found` | Alvo da avaliacao nao encontrado |
| 409 | `/errors/review-already-exists` | Ja avaliou este alvo |
| 409 | `/errors/self-review` | Nao pode avaliar a si mesmo |
| 409 | `/errors/report-already-exists` | Ja reportou esta review |
| 422 | `/errors/insufficient-coexistence` | Minimo de 2 semanas de convivencia nao atingido (RN32) |
| 422 | `/errors/no-shared-static` | Reviewer e target nunca estiveram na mesma static |
