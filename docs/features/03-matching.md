# Feature 03 - Matching

## 1. Visao Geral

O modulo de Matching implementa o sistema de recomendacao automatica que conecta jogadores e statics com base em multiplos criterios ponderados. O algoritmo calcula scores de compatibilidade entre jogadores LFS (Looking for Static) e statics com vagas abertas, gerando sugestoes que facilitam o recrutamento. O calculo e executado diariamente via job agendado e os resultados sao cacheados no Redis.

### 1.1 Rastreabilidade

| Tipo                  | IDs                                                                                                   |
| --------------------- | ----------------------------------------------------------------------------------------------------- |
| **Regras de Negocio** | RN19, RN20, RN21, RN22, RN23                                                                          |
| **Casos de Uso**      | UC12 (Ver Sugestoes de Match), UC13 (Configurar Preferencias de Match), UC14 (Interagir com Sugestao) |

---

## 2. Backend

### 2.1 Entities (JPA)

#### MatchSuggestion

```java
@Entity
@Table(name = "match_suggestions")
public class MatchSuggestion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character; // jogador que recebe a sugestao

    @Column(name = "target_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MatchTargetType targetType; // STATIC ou PLAYER

    @Column(name = "target_id", nullable = false)
    private UUID targetId; // ID da static ou do character alvo

    @Column(nullable = false)
    private double score; // 0.0 a 100.0

    @Column(columnDefinition = "jsonb")
    private String criteria; // breakdown detalhado dos scores por criterio

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MatchStatus status; // ACTIVE, DISMISSED, APPLIED, EXPIRED

    @Column(name = "expires_at")
    private Instant expiresAt;
}

public enum MatchTargetType {
    STATIC,  // sugestao de static para o jogador
    PLAYER   // sugestao de jogador para a static (visto pelo lider)
}

public enum MatchStatus {
    ACTIVE,     // sugestao visivel
    DISMISSED,  // descartada pelo usuario
    APPLIED,    // usuario se candidatou/convidou
    EXPIRED     // expirada (recalculada)
}
```

#### MatchPreference

```java
@Entity
@Table(name = "match_preferences")
public class MatchPreference extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false, unique = true)
    private Character character;

    @Column(name = "criteria_weights", columnDefinition = "jsonb")
    private String criteriaWeights;
    // JSON: {"roleJob": 25, "schedule": 25, "dataCenter": 15, "language": 10,
    //        "progression": 10, "contentScope": 10, "playstyle": 5}

    @Column(name = "mandatory_filters", columnDefinition = "jsonb")
    private String mandatoryFilters;
    // JSON: {"sameDataCenter": true, "languages": ["en"], "contentScope": ["SAVAGE"]}

    @Column(name = "blacklist", columnDefinition = "jsonb")
    private String blacklist;
    // JSON: {"staticIds": [...], "characterIds": [...]}

    @Column(name = "preferred_schedule", columnDefinition = "jsonb")
    private String preferredSchedule;
    // JSON: {"monday": ["20:00-23:00"], "wednesday": ["20:00-23:00"]}

    @Column(name = "preferred_roles", columnDefinition = "jsonb")
    private String preferredRoles;
    // JSON: ["HEALER", "DPS"]
}
```

### 2.2 DTOs (Java Records)

#### Request DTOs

```java
// Atualizar preferencias de match
public record UpdateMatchPreferencesRequest(
    Map<String, Integer> criteriaWeights, // pesos por criterio (somam 100)

    MandatoryFiltersDto mandatoryFilters,

    List<UUID> blacklistStaticIds,
    List<UUID> blacklistCharacterIds,

    String preferredSchedule, // JSON de horarios preferidos
    List<Role> preferredRoles
) {}

public record MandatoryFiltersDto(
    boolean sameDataCenter,
    List<String> languages,
    List<String> contentScope,
    String minParseTier
) {}
```

#### Response DTOs

```java
// Sugestao de match
public record MatchSuggestionResponse(
    UUID id,
    MatchTargetType targetType,
    double score,
    MatchStatus status,
    MatchCriteriaBreakdown criteria,
    // Se targetType == STATIC:
    StaticSummaryResponse static_,
    // Se targetType == PLAYER:
    CharacterSummaryResponse character,
    Instant createdAt,
    Instant expiresAt
) {}

// Breakdown dos criterios
public record MatchCriteriaBreakdown(
    CriterionScore roleJob,        // 25%
    CriterionScore schedule,       // 25%
    CriterionScore dataCenter,     // 15%
    CriterionScore language,       // 10%
    CriterionScore progression,    // 10%
    CriterionScore contentScope,   // 10%
    CriterionScore playstyle       //  5%
) {}

public record CriterionScore(
    String name,
    double weight,      // peso percentual
    double rawScore,    // score bruto (0-100)
    double weightedScore, // rawScore * weight / 100
    String detail       // descricao textual do match
) {}

// Preferencias
public record MatchPreferencesResponse(
    UUID id,
    Map<String, Integer> criteriaWeights,
    MandatoryFiltersDto mandatoryFilters,
    List<UUID> blacklistStaticIds,
    List<UUID> blacklistCharacterIds,
    String preferredSchedule,
    List<Role> preferredRoles
) {}
```

#### Mapper (MapStruct)

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {StaticMapper.class, CharacterMapper.class})
public interface MatchMapper {
    MatchSuggestionResponse toResponse(MatchSuggestion suggestion);
    MatchPreferencesResponse toPreferencesResponse(MatchPreference preference);
}
```

### 2.3 Repository

```java
public interface MatchSuggestionRepository extends JpaRepository<MatchSuggestion, UUID> {

    @Query("SELECT m FROM MatchSuggestion m WHERE m.character.id = :charId AND m.status = 'ACTIVE' " +
           "ORDER BY m.score DESC")
    List<MatchSuggestion> findActiveByCharacterId(@Param("charId") UUID characterId);

    @Query("SELECT m FROM MatchSuggestion m WHERE m.character.id = :charId AND m.status = 'ACTIVE' " +
           "ORDER BY m.score DESC")
    Page<MatchSuggestion> findActiveByCharacterId(@Param("charId") UUID characterId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM MatchSuggestion m WHERE m.character.id = :charId AND m.status = 'ACTIVE'")
    int countActiveByCharacterId(@Param("charId") UUID characterId);

    @Modifying
    @Query("UPDATE MatchSuggestion m SET m.status = 'EXPIRED' WHERE m.character.id = :charId AND m.status = 'ACTIVE'")
    void expireAllByCharacterId(@Param("charId") UUID characterId);

    @Modifying
    @Query("UPDATE MatchSuggestion m SET m.status = 'EXPIRED' WHERE m.expiresAt < :now AND m.status = 'ACTIVE'")
    void expireOld(@Param("now") Instant now);

    boolean existsByCharacterIdAndTargetIdAndStatusIn(UUID characterId, UUID targetId, List<MatchStatus> statuses);
}

public interface MatchPreferenceRepository extends JpaRepository<MatchPreference, UUID> {

    Optional<MatchPreference> findByCharacterId(UUID characterId);
}
```

### 2.4 Service

#### MatchingService

```java
@Service
@Transactional
public class MatchingService {

    /**
     * Obter sugestoes de match do usuario.
     * Fluxo:
     * 1. Busca character principal do usuario
     * 2. Verifica que e verificado (RN02)
     * 3. Verifica limite free/premium (RN20): free = 5/semana
     * 4. Busca sugestoes ACTIVE do cache ou DB, ordenadas por score DESC
     * 5. Filtra sugestoes abaixo de 40% (RN22)
     * 6. Para free: incrementa contador Redis count:matches:{userId}:{week}
     * 7. Retorna lista paginada
     *
     * Regras: RN20 (limite free), RN22 (threshold 40%)
     */
    @Transactional(readOnly = true)
    public Page<MatchSuggestionResponse> getMatches(UUID userId, Pageable pageable);

    /**
     * Obter detalhe de um match especifico.
     * Inclui breakdown completo dos criterios.
     */
    @Transactional(readOnly = true)
    public MatchSuggestionResponse getMatch(UUID userId, UUID matchId);

    /**
     * Descartar uma sugestao de match.
     * Atualiza status para DISMISSED. Nao aparece mais na lista.
     */
    public void dismissMatch(UUID userId, UUID matchId);

    /**
     * Obter preferencias de matching.
     * Se nao existem, retorna valores default.
     */
    @Transactional(readOnly = true)
    public MatchPreferencesResponse getPreferences(UUID userId);

    /**
     * Atualizar preferencias de matching.
     * Fluxo:
     * 1. Valida que os pesos somam 100
     * 2. Cria ou atualiza MatchPreference
     * 3. Invalida cache de matches do usuario
     * 4. Publica MatchPreferencesUpdatedEvent (pode triggerar recalculo)
     *
     * Regras: RN21 (pesos configuraveis)
     */
    public MatchPreferencesResponse updatePreferences(UUID userId, UpdateMatchPreferencesRequest request);
}
```

#### MatchCalculationService

```java
@Service
public class MatchCalculationService {

    /**
     * Calcular score de compatibilidade entre um character e uma static.
     *
     * Algoritmo (RN19, RN21, RN23):
     *
     * 1. ROLE/JOB (peso padrao: 25%)
     *    - O character tem um job compativel com alguma vaga aberta?
     *    - Match exato do job preferido = 100
     *    - Match de role (TANK/HEALER/DPS) mas job diferente = 70
     *    - Sem match = 0
     *
     * 2. SCHEDULE (peso padrao: 25%)
     *    - Sobreposicao de horarios disponiveis
     *    - Calcula % de horas sobrepostas vs horas da static
     *    - >= 80% overlap = 100
     *    - 60-79% = 75
     *    - 40-59% = 50
     *    - < 40% = 0
     *
     * 3. DATA CENTER (peso padrao: 15%)
     *    - Mesmo server = 100
     *    - Mesmo Data Center, server diferente = 80
     *    - Data Center diferente, mesma regiao = 40
     *    - Regiao diferente = 0 (RN23: prioriza mesmo DC)
     *
     * 4. LANGUAGE (peso padrao: 10%)
     *    - Idioma do character coincide com idioma da static = 100
     *    - Character fala o idioma como secundario = 70
     *    - Sem match de idioma = 0
     *
     * 5. PROGRESSION (peso padrao: 10%)
     *    - Nivel de progressao do character vs requisitos da vaga
     *    - Mesmo nivel ou acima = 100
     *    - 1 tier abaixo = 60
     *    - 2+ tiers abaixo = 20
     *
     * 6. CONTENT SCOPE (peso padrao: 10%)
     *    - Sobreposicao de tipos de conteudo desejados
     *    - Match total = 100
     *    - Match parcial = 50
     *    - Sem match = 0
     *
     * 7. PLAYSTYLE (peso padrao: 5%)
     *    - Nivel de seriedade (casual, midcore, hardcore) compativel
     *    - Parse tier do character vs expectativa da static
     *    - Match = 100
     *    - Adjacente = 60
     *    - Incompativel = 0
     *
     * Score final = SUM(rawScore[i] * weight[i] / 100) para i = 1..7
     *
     * Se score < 40: nao gera sugestao (RN22)
     */
    public MatchResult calculateScore(Character character, Static static_, MatchPreference preference);

    /**
     * Calcular score entre dois jogadores (para matching LFS bidirecional).
     * Similar ao calculo character-static, mas sem o criterio de role/job
     * (substituido por complementaridade de roles).
     */
    public MatchResult calculatePlayerScore(Character char1, Character char2, MatchPreference pref1);
}

public record MatchResult(
    double totalScore,
    MatchCriteriaBreakdown breakdown,
    boolean meetsThreshold // score >= 40
) {}
```

#### MatchScheduler

```java
@Component
public class MatchScheduler {

    /**
     * Job de recalculo de matches.
     * Executa diariamente as 03:00 UTC.
     *
     * Fluxo:
     * 1. Adquire lock distribuido: lock:matching:run (TTL 10min) -- evita execucao concorrente
     * 2. Se lock nao adquirido, aborta (outra instancia ja esta executando)
     * 3. Busca todos os characters verificados com lookingForStatic=true
     * 4. Busca todas as statics com vagas abertas (status RECRUITING)
     * 5. Para cada character:
     *    a. Busca MatchPreference (ou usa defaults)
     *    b. Expira sugestoes ativas anteriores
     *    c. Calcula score com cada static compativel (mesmo DC primeiro - RN23)
     *    d. Filtra scores abaixo de 40% (RN22)
     *    e. Salva top N sugestoes como MatchSuggestion (ACTIVE)
     *    f. Invalida cache: match:{characterId}
     * 6. Para cada static com vagas:
     *    a. Calcula score com cada character LFS
     *    b. Salva sugestoes para o lider ver
     * 7. Libera lock distribuido
     * 8. Publica MatchingCompletedEvent (para notificacoes de novas sugestoes)
     */
    @Scheduled(cron = "0 0 3 * * *") // 03:00 UTC diario
    public void recalculateMatches();
}
```

#### Eventos Publicados

| Evento                         | Publicado por                       | Dados              | Consumido por                                                 |
| ------------------------------ | ----------------------------------- | ------------------ | ------------------------------------------------------------- |
| `MatchPreferencesUpdatedEvent` | `MatchingService.updatePreferences` | characterId        | `MatchScheduler` (pode triggerar recalculo individual)        |
| `MatchingCompletedEvent`       | `MatchScheduler`                    | totalSuggestions   | `NotificationService` (notifica usuarios com novas sugestoes) |
| `NewMatchSuggestionEvent`      | `MatchScheduler` (por usuario)      | userId, matchCount | `NotificationService` (push notification)                     |

#### Eventos Consumidos

| Evento                   | Origem             | Acao                                                |
| ------------------------ | ------------------ | --------------------------------------------------- |
| `CharacterVerifiedEvent` | `CharacterService` | Ativa matching para o character (calcula sugestoes) |
| `ProfileUpdatedEvent`    | `ProfileService`   | Recalcula matches do character                      |
| `VacancyCreatedEvent`    | `VacancyService`   | Recalcula matches para a static                     |
| `StaticDissolvedEvent`   | `StaticService`    | Remove sugestoes da static dissolvida               |

### 2.5 Controller

```
@RestController
@RequestMapping("/api/v1/matches")
```

| Metodo | Path            | Descricao              | Auth | Response Body                   | Status |
| ------ | --------------- | ---------------------- | ---- | ------------------------------- | ------ |
| GET    | `/`             | Minhas sugestoes       | Sim  | `Page<MatchSuggestionResponse>` | 200    |
| GET    | `/{id}`         | Detalhe do match       | Sim  | `MatchSuggestionResponse`       | 200    |
| PUT    | `/{id}/dismiss` | Descartar match        | Sim  | -                               | 204    |
| GET    | `/preferences`  | Minhas preferencias    | Sim  | `MatchPreferencesResponse`      | 200    |
| PUT    | `/preferences`  | Atualizar preferencias | Sim  | `MatchPreferencesResponse`      | 200    |

**Exemplos de Request/Response:**

```json
// GET /api/v1/matches?page=0&size=10
// Response (200):
{
  "content": [
    {
      "id": "aa0e8400-e29b-41d4-a716-446655440030",
      "targetType": "STATIC",
      "score": 87.5,
      "status": "ACTIVE",
      "criteria": {
        "roleJob": {
          "name": "Role/Job",
          "weight": 25,
          "rawScore": 100,
          "weightedScore": 25.0,
          "detail": "WHM matches HEALER vacancy (exact job match)"
        },
        "schedule": {
          "name": "Schedule",
          "weight": 25,
          "rawScore": 90,
          "weightedScore": 22.5,
          "detail": "90% schedule overlap (Tue/Thu 20:00-23:00)"
        },
        "dataCenter": {
          "name": "Data Center",
          "weight": 15,
          "rawScore": 100,
          "weightedScore": 15.0,
          "detail": "Same Data Center (Primal)"
        },
        "language": {
          "name": "Language",
          "weight": 10,
          "rawScore": 100,
          "weightedScore": 10.0,
          "detail": "Language match (English)"
        },
        "progression": {
          "name": "Progression",
          "weight": 10,
          "rawScore": 80,
          "weightedScore": 8.0,
          "detail": "Character at P11S, static requires P12S prog"
        },
        "contentScope": {
          "name": "Content Scope",
          "weight": 10,
          "rawScore": 100,
          "weightedScore": 10.0,
          "detail": "Content match (Savage)"
        },
        "playstyle": {
          "name": "Playstyle",
          "weight": 5,
          "rawScore": 60,
          "weightedScore": 3.0,
          "detail": "Character casual-midcore, static midcore (adjacent)"
        }
      },
      "static_": {
        "id": "880e8400-...",
        "name": "Heavens' Gate",
        "contentScope": ["SAVAGE"],
        "language": "en",
        "status": "RECRUITING",
        "maxMembers": 8,
        "currentMembers": 6,
        "dataCenter": "Primal",
        "server": "Lamia",
        "openVacancies": 2,
        "createdAt": "2026-02-01T00:00:00Z"
      },
      "createdAt": "2026-03-04T03:00:00Z",
      "expiresAt": "2026-03-05T03:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 3,
  "totalPages": 1,
  "hasNext": false
}
```

```json
// PUT /api/v1/matches/preferences
// Request:
{
  "criteriaWeights": {
    "roleJob": 30,
    "schedule": 30,
    "dataCenter": 15,
    "language": 5,
    "progression": 10,
    "contentScope": 5,
    "playstyle": 5
  },
  "mandatoryFilters": {
    "sameDataCenter": true,
    "languages": ["en"],
    "contentScope": ["SAVAGE"]
  },
  "blacklistStaticIds": [],
  "blacklistCharacterIds": [],
  "preferredSchedule": "{\"tuesday\": [\"20:00-23:00\"], \"thursday\": [\"20:00-23:00\"]}",
  "preferredRoles": ["HEALER"]
}
```

### 2.6 Seguranca e Autorizacao

```java
// Todos os endpoints requerem autenticacao
@PreAuthorize("isAuthenticated()")

// Verificacao de ownership no service:
// - Cada usuario so ve suas proprias sugestoes
// - Character deve pertencer ao usuario autenticado
// - Character deve ser verificado (RN02)
```

### 2.7 Validacoes

| Campo                          | Regra                                                | Contexto                      |
| ------------------------------ | ---------------------------------------------------- | ----------------------------- |
| `criteriaWeights`              | Valores devem somar exatamente 100                   | UpdateMatchPreferencesRequest |
| `criteriaWeights` (cada valor) | Entre 0 e 100, inteiro                               | UpdateMatchPreferencesRequest |
| Character verificado           | Matching so funciona com character verificado (RN02) | MatchingService               |
| Limite free                    | Max 5 sugestoes/semana para free (RN20)              | MatchingService.getMatches    |
| Threshold 40%                  | Sugestoes abaixo de 40% nao sao exibidas (RN22)      | MatchCalculationService       |
| Blacklist                      | Statics/characters na blacklist nao aparecem         | MatchCalculationService       |

### 2.8 Cache (Redis)

| Chave                             | Tipo          | TTL      | Descricao                                                | Invalidacao                                         |
| --------------------------------- | ------------- | -------- | -------------------------------------------------------- | --------------------------------------------------- |
| `match:{characterId}`             | String (JSON) | 24 horas | Cache das sugestoes de match calculadas                  | Recalculo diario, dismiss, preferencias atualizadas |
| `count:matches:{userId}:{week}`   | Counter       | 7 dias   | Contador de sugestoes visualizadas (limite free - RN20)  | Reset semanal automatico                            |
| `lock:matching:run`               | String        | 10 min   | Lock distribuido para evitar execucao concorrente do job | Liberado apos conclusao ou timeout                  |
| `match:preferences:{characterId}` | String (JSON) | 1 hora   | Cache das preferencias                                   | Atualizacao de preferencias                         |

### 2.9 Jobs Agendados

| Job              | Frequencia         | Descricao                                                                                                                                                                      |
| ---------------- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `MatchScheduler` | Diario (03:00 UTC) | Recalcula sugestoes de match para todos os jogadores LFS e statics com vagas. Usa lock distribuido. Prioriza matches no mesmo Data Center (RN23). Filtra abaixo de 40% (RN22). |

---

## 3. Frontend

### 3.1 Paginas e Rotas

| Rota                                   | Renderizacao | Descricao                           |
| -------------------------------------- | ------------ | ----------------------------------- |
| `/[locale]/(main)/matches`             | CSR          | Lista de sugestoes de match         |
| `/[locale]/(main)/matches/preferences` | CSR          | Configurar preferencias de matching |

### 3.2 Componentes React

```
matches/page.tsx                    -- Client Component
  MatchesList                       -- Lista de sugestoes
    MatchCard                       -- Card de sugestao
      CompatibilityScore            -- Score circular (87.5%)
      TargetPreview                 -- Preview da static ou jogador
        StaticCard / PlayerCard
      QuickActions                  -- Aplicar, Ver detalhes, Descartar
    MatchDetail (modal/drawer)      -- Detalhe expandido
      CompatibilityBreakdown        -- Grafico radar com os 7 criterios
        CriterionBar                -- Barra de progresso por criterio
      TargetFullProfile             -- Perfil completo do alvo
      ActionButtons                 -- Aplicar a vaga / Convidar / Descartar
  EmptyState                        -- Estado vazio (sem sugestoes)
  PremiumUpsell                     -- Banner de upgrade (se free e atingiu limite)

matches/preferences/page.tsx        -- Client Component
  PreferencesForm                   -- Formulario de preferencias
    WeightsEditor                   -- Editor de pesos (sliders que somam 100)
      WeightSlider                  -- Slider individual por criterio
      WeightsTotal                  -- Indicador de total (deve ser 100)
    MandatoryFilters                -- Filtros obrigatorios
      DataCenterToggle              -- Apenas mesmo DC
      LanguageSelect                -- Idiomas aceitos
      ContentScopeSelect            -- Tipos de conteudo
    SchedulePreference              -- Horarios preferidos
      AvailabilityGrid              -- Grade de horarios
    BlacklistManager                -- Gerenciar blacklist
      BlacklistEntry                -- Entrada de blacklist
    SaveButton                      -- Salvar preferencias
```

### 3.3 Estado e Data Fetching

#### Hooks TanStack Query

```typescript
// hooks/use-matches.ts

function useMatches(page: number = 0) {
  return useQuery({
    queryKey: ["matches", page],
    queryFn: () => api.get(`/matches?page=${page}&size=10&sort=score,desc`),
    enabled: authStore.isAuthenticated,
  });
}

function useMatch(id: string) {
  return useQuery({
    queryKey: ["matches", id],
    queryFn: () => api.get(`/matches/${id}`),
    enabled: !!id,
  });
}

function useDismissMatch() {
  return useMutation({
    mutationFn: (id: string) => api.put(`/matches/${id}/dismiss`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["matches"] });
    },
  });
}

function useMatchPreferences() {
  return useQuery({
    queryKey: ["matches", "preferences"],
    queryFn: () => api.get("/matches/preferences"),
    enabled: authStore.isAuthenticated,
  });
}

function useUpdateMatchPreferences() {
  return useMutation({
    mutationFn: (data: UpdateMatchPreferencesRequest) => api.put("/matches/preferences", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["matches", "preferences"] });
      queryClient.invalidateQueries({ queryKey: ["matches"] });
    },
  });
}
```

#### WebSocket (notificacoes de novos matches)

```typescript
// Novas sugestoes sao notificadas via canal pessoal
// /user/queue/matches
stompClient.subscribe("/user/queue/matches", (message) => {
  const data = JSON.parse(message.body);
  // { type: "NEW_MATCHES", count: 3 }
  queryClient.invalidateQueries({ queryKey: ["matches"] });
  toast.info(`${data.count} new match suggestions available!`);
});
```

### 3.4 Formularios e Validacao (Zod)

```typescript
// schemas/match-preferences.ts
import { z } from "zod";

export const matchPreferencesSchema = z.object({
  criteriaWeights: z
    .object({
      roleJob: z.number().int().min(0).max(100),
      schedule: z.number().int().min(0).max(100),
      dataCenter: z.number().int().min(0).max(100),
      language: z.number().int().min(0).max(100),
      progression: z.number().int().min(0).max(100),
      contentScope: z.number().int().min(0).max(100),
      playstyle: z.number().int().min(0).max(100),
    })
    .refine((weights) => Object.values(weights).reduce((a, b) => a + b, 0) === 100, {
      message: "Criteria weights must sum to 100",
    }),
  mandatoryFilters: z
    .object({
      sameDataCenter: z.boolean().default(true),
      languages: z.array(z.string()).optional(),
      contentScope: z.array(z.enum(["SAVAGE", "ULTIMATE", "EXTREME", "CRITERION"])).optional(),
      minParseTier: z.enum(["GRAY", "GREEN", "BLUE", "PURPLE", "ORANGE", "PINK"]).optional(),
    })
    .optional(),
  blacklistStaticIds: z.array(z.string().uuid()).optional(),
  blacklistCharacterIds: z.array(z.string().uuid()).optional(),
  preferredSchedule: z.any().optional(),
  preferredRoles: z.array(z.enum(["TANK", "HEALER", "DPS"])).optional(),
});
```

### 3.5 WebSocket

| Destination           | Tipo      | Descricao                               |
| --------------------- | --------- | --------------------------------------- |
| `/user/queue/matches` | SUBSCRIBE | Notificacao de novas sugestoes de match |

### 3.6 Internacionalizacao

**Namespaces i18n:**

| Namespace             | Uso                                            |
| --------------------- | ---------------------------------------------- |
| `match.*`             | Pagina de sugestoes                            |
| `match.criteria.*`    | Nomes dos criterios (Role/Job, Schedule, etc.) |
| `match.preferences.*` | Pagina de preferencias                         |
| `match.status.*`      | Status das sugestoes                           |
| `match.empty.*`       | Estados vazios                                 |
| `match.premium.*`     | Mensagens de upsell                            |

---

## 4. Banco de Dados

### 4.1 DDL

```sql
-- Migration: V4__create_matching.sql

CREATE TABLE match_suggestions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id    UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    target_type     VARCHAR(20) NOT NULL, -- STATIC, PLAYER
    target_id       UUID NOT NULL,
    score           DOUBLE PRECISION NOT NULL,
    criteria        JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE match_preferences (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id        UUID NOT NULL UNIQUE REFERENCES characters(id) ON DELETE CASCADE,
    criteria_weights    JSONB,
    mandatory_filters   JSONB,
    blacklist           JSONB,
    preferred_schedule  JSONB,
    preferred_roles     JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indices
CREATE INDEX idx_match_suggestions_character_status ON match_suggestions(character_id, status);
CREATE INDEX idx_match_suggestions_target ON match_suggestions(target_type, target_id);
CREATE INDEX idx_match_suggestions_score ON match_suggestions(score DESC) WHERE status = 'ACTIVE';
CREATE INDEX idx_match_suggestions_expires ON match_suggestions(expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_match_preferences_character ON match_preferences(character_id);
```

### 4.2 Indices

| Tabela              | Indice                   | Tipo       | Justificativa                 |
| ------------------- | ------------------------ | ---------- | ----------------------------- |
| `match_suggestions` | `character_id, status`   | BTREE      | Sugestoes ativas do jogador   |
| `match_suggestions` | `target_type, target_id` | BTREE      | Sugestoes por alvo            |
| `match_suggestions` | `score` (partial)        | BTREE DESC | Ordenacao por score           |
| `match_suggestions` | `expires_at` (partial)   | BTREE      | Expirar sugestoes antigas     |
| `match_preferences` | `character_id`           | UNIQUE     | Uma preferencia por character |

---

## 5. Integracao Entre Modulos

### 5.1 Dependencias

```
matching
    |
    +--> auth-profile (Character para calculo de score)
    |
    +--> static-management (Static, vacancies para calcular matches)
    |
    +--> premium (verifica limite free/premium - RN20)
    |
    +--> notification (notifica novas sugestoes)
    |
    <-- auth-profile (CharacterVerifiedEvent, ProfileUpdatedEvent)
    |
    <-- static-management (VacancyCreatedEvent, StaticDissolvedEvent)
```

### 5.2 Eventos

| Direcao | Evento                    | Acao                                                |
| ------- | ------------------------- | --------------------------------------------------- |
| Entrada | `CharacterVerifiedEvent`  | Calcula sugestoes para o character recem-verificado |
| Entrada | `ProfileUpdatedEvent`     | Recalcula sugestoes do character atualizado         |
| Entrada | `VacancyCreatedEvent`     | Recalcula sugestoes para a static com nova vaga     |
| Entrada | `StaticDissolvedEvent`    | Remove todas as sugestoes da static dissolvida      |
| Saida   | `NewMatchSuggestionEvent` | Notifica usuario de novas sugestoes via WebSocket   |
| Saida   | `MatchingCompletedEvent`  | Log de conclusao do job diario                      |

---

## 6. Tratamento de Erros

| HTTP | Tipo RFC 7807                     | Cenario                                  |
| ---- | --------------------------------- | ---------------------------------------- |
| 400  | `/errors/validation`              | Campos invalidos                         |
| 400  | `/errors/weights-sum-invalid`     | Pesos dos criterios nao somam 100        |
| 403  | `/errors/character-not-verified`  | Character nao verificado (RN02)          |
| 403  | `/errors/premium-required`        | Limite free de sugestoes atingido (RN20) |
| 404  | `/errors/match-not-found`         | Sugestao nao encontrada                  |
| 404  | `/errors/preferences-not-found`   | Preferencias nao encontradas             |
| 422  | `/errors/match-already-dismissed` | Sugestao ja foi descartada               |
