# Feature 04 - Calendario

## 1. Visao Geral

O modulo de Calendario gerencia os eventos de raid das statics e eventos publicos PUG (Pick-Up Group). Inclui a criacao e gestao de eventos, confirmacao de presenca, lembretes automaticos, eventos recorrentes, e exportacao iCal. Eventos PUG publicos permitem que jogadores fora de statics participem de conteudo organizado.

### 1.1 Rastreabilidade

| Tipo                  | IDs                                                                                                                                     |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| **Regras de Negocio** | RN24, RN25, RN26, RN27, RN28, RN29                                                                                                      |
| **Casos de Uso**      | UC15 (Criar Evento de Raid), UC16 (Confirmar Presenca), UC17 (Criar Evento PUG), UC18 (Inscrever-se em PUG), UC19 (Exportar Calendario) |

---

## 2. Backend

### 2.1 Entities (JPA)

#### RaidEvent

```java
@Entity
@Table(name = "raid_events")
public class RaidEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "static_id")
    private Static static_; // null para eventos PUG

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "content_type", length = 50)
    private String contentType; // SAVAGE, ULTIMATE, EXTREME, CRITERION

    @Column(length = 100)
    private String encounter; // ex: "P12S", "DSR", "EX4"

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(nullable = false, length = 50)
    private String timezone; // ex: "America/New_York", "Europe/Berlin"

    @Column(name = "is_recurring", nullable = false)
    private boolean isRecurring = false;

    @Column(name = "recurrence_rule", length = 200)
    private String recurrenceRule; // iCal RRULE format: "FREQ=WEEKLY;BYDAY=TU,TH"

    @Column(name = "recurrence_end")
    private Instant recurrenceEnd;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "max_participants")
    private Integer maxParticipants; // para eventos PUG

    @Column(name = "data_center", length = 20)
    private String dataCenter; // para filtragem de PUGs por DC

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attendance> attendances = new ArrayList<>();
}
```

#### Attendance

```java
@Entity
@Table(name = "attendances")
public class Attendance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private RaidEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status; // CONFIRMED, TENTATIVE, ABSENT, REGISTERED, WAITLISTED

    @Column(length = 500)
    private String note;

    @Column(name = "responded_at")
    private Instant respondedAt;
}

public enum AttendanceStatus {
    CONFIRMED,   // confirmou presenca
    TENTATIVE,   // talvez
    ABSENT,      // ausente
    REGISTERED,  // inscrito em PUG (aguardando)
    WAITLISTED   // na lista de espera (PUG lotado)
}
```

### 2.2 DTOs (Java Records)

#### Request DTOs

```java
// Criar/Atualizar evento
public record CreateEventRequest(
    @NotBlank @Size(max = 200)
    String title,

    @Size(max = 2000)
    String description,

    @Size(max = 50)
    String contentType,

    @Size(max = 100)
    String encounter,

    @NotNull
    Instant startTime,

    @NotNull
    Instant endTime,

    @NotBlank @Size(max = 50)
    String timezone,

    boolean isRecurring,

    @Size(max = 200)
    String recurrenceRule,

    Instant recurrenceEnd,

    boolean isPublic,

    Integer maxParticipants, // obrigatorio para PUG

    @Size(max = 20)
    String dataCenter // obrigatorio para PUG
) {}

// Atualizar presenca
public record UpdateAttendanceRequest(
    @NotNull
    AttendanceStatus status,

    @Size(max = 500)
    String note
) {}
```

#### Response DTOs

```java
// Evento
public record EventResponse(
    UUID id,
    UUID staticId,
    String staticName,
    String title,
    String description,
    String contentType,
    String encounter,
    Instant startTime,
    Instant endTime,
    String timezone,
    boolean isRecurring,
    String recurrenceRule,
    boolean isPublic,
    Integer maxParticipants,
    int currentParticipants,
    String dataCenter,
    UUID createdById,
    boolean isCanceled,
    List<AttendanceResponse> attendances,
    Instant createdAt
) {}

// Evento resumido (para listagens e calendario)
public record EventSummaryResponse(
    UUID id,
    UUID staticId,
    String staticName,
    String title,
    String contentType,
    String encounter,
    Instant startTime,
    Instant endTime,
    String timezone,
    boolean isPublic,
    boolean isCanceled,
    AttendanceStatus myAttendance // status do usuario autenticado, se aplicavel
) {}

// Presenca
public record AttendanceResponse(
    UUID id,
    CharacterSummaryResponse character,
    AttendanceStatus status,
    String note,
    Instant respondedAt
) {}

// Calendario consolidado
public record CalendarResponse(
    List<EventSummaryResponse> events,
    String startDate,
    String endDate
) {}

// iCal export
// Retorna text/calendar (VCALENDAR format)
```

#### Mapper (MapStruct)

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CharacterMapper.class})
public interface EventMapper {
    EventResponse toResponse(RaidEvent event);
    EventSummaryResponse toSummary(RaidEvent event);
    AttendanceResponse toAttendanceResponse(Attendance attendance);
}
```

### 2.3 Repository

```java
public interface RaidEventRepository extends JpaRepository<RaidEvent, UUID> {

    @Query("SELECT e FROM RaidEvent e WHERE e.static_.id = :staticId AND e.canceledAt IS NULL " +
           "AND e.startTime >= :start ORDER BY e.startTime ASC")
    List<RaidEvent> findUpcomingByStaticId(@Param("staticId") UUID staticId,
                                           @Param("start") Instant start);

    @Query("SELECT e FROM RaidEvent e WHERE e.static_.id = :staticId AND e.canceledAt IS NULL " +
           "AND e.startTime BETWEEN :start AND :end ORDER BY e.startTime ASC")
    List<RaidEvent> findByStaticIdAndDateRange(@Param("staticId") UUID staticId,
                                               @Param("start") Instant start,
                                               @Param("end") Instant end);

    @Query("SELECT e FROM RaidEvent e WHERE e.isPublic = true AND e.canceledAt IS NULL " +
           "AND e.dataCenter = :dc AND e.startTime >= :now ORDER BY e.startTime ASC")
    Page<RaidEvent> findPublicByDataCenter(@Param("dc") String dataCenter,
                                           @Param("now") Instant now,
                                           Pageable pageable);

    @Query("SELECT e FROM RaidEvent e WHERE e.canceledAt IS NULL " +
           "AND e.startTime BETWEEN :reminderStart AND :reminderEnd " +
           "AND e.id NOT IN (SELECT n.referenceId FROM Notification n WHERE n.type = :reminderType)")
    List<RaidEvent> findEventsNeedingReminder(@Param("reminderStart") Instant start,
                                              @Param("reminderEnd") Instant end,
                                              @Param("reminderType") String type);

    @Query("SELECT e FROM RaidEvent e JOIN e.attendances a " +
           "WHERE a.character.user.id = :userId AND e.canceledAt IS NULL " +
           "AND e.startTime BETWEEN :start AND :end ORDER BY e.startTime ASC")
    List<RaidEvent> findUserEventsInRange(@Param("userId") UUID userId,
                                          @Param("start") Instant start,
                                          @Param("end") Instant end);
}

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByEventIdAndCharacterId(UUID eventId, UUID characterId);

    @Query("SELECT a FROM Attendance a WHERE a.event.id = :eventId ORDER BY a.respondedAt ASC")
    List<Attendance> findByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.event.id = :eventId AND a.status IN ('CONFIRMED', 'REGISTERED')")
    int countConfirmedByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT a FROM Attendance a WHERE a.event.id = :eventId AND a.status = 'ABSENT'")
    List<Attendance> findAbsentByEventId(@Param("eventId") UUID eventId);
}
```

### 2.4 Service

#### EventService

```java
@Service
@Transactional
public class EventService {

    /**
     * Criar evento de raid para a static.
     * Fluxo:
     * 1. Verifica que o usuario e lider da static
     * 2. Valida que startTime < endTime
     * 3. Se isRecurring, valida recurrenceRule (formato RRULE)
     * 4. Cria RaidEvent vinculado a static
     * 5. Cria Attendance para cada membro ativo com status TENTATIVE
     * 6. Notifica membros da static via WebSocket (/topic/static.{staticId})
     * 7. Publica EventCreatedEvent
     *
     * Regras: RN24 (calendario compartilhado)
     */
    public EventResponse createStaticEvent(UUID userId, UUID staticId, CreateEventRequest request);

    /**
     * Criar evento PUG publico.
     * Fluxo:
     * 1. Verifica que o usuario e premium (@RequiresPremium)
     * 2. Valida maxParticipants e dataCenter (obrigatorios para PUG)
     * 3. Cria RaidEvent com isPublic=true, static_=null
     * 4. Publica PugEventCreatedEvent
     *
     * Regras: RN26 (PUGs publicos), RN27 (vagas limitadas)
     */
    @RequiresPremium(message = "Creating PUG events requires premium")
    public EventResponse createPugEvent(UUID userId, CreateEventRequest request);

    /**
     * Atualizar evento.
     * Verifica que o usuario e o criador ou lider da static.
     * Notifica participantes via WebSocket.
     */
    public EventResponse updateEvent(UUID userId, UUID eventId, CreateEventRequest request);

    /**
     * Cancelar evento (soft cancel).
     * Marca canceledAt e notifica todos os participantes.
     */
    public void cancelEvent(UUID userId, UUID eventId);

    /**
     * Obter eventos da static.
     * Retorna eventos no range de datas especificado.
     *
     * Regras: RN24 (visivel para todos os membros)
     */
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> getStaticEvents(UUID userId, UUID staticId,
                                                       Instant start, Instant end);

    /**
     * Obter eventos PUG publicos.
     * Filtra por Data Center (RN26).
     */
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> getPublicEvents(String dataCenter, Pageable pageable);

    /**
     * Obter calendario consolidado do usuario.
     * Agrega eventos de todas as statics do usuario + PUGs inscritos.
     */
    @Transactional(readOnly = true)
    public CalendarResponse getMyCalendar(UUID userId, Instant start, Instant end);

    /**
     * Exportar calendario em formato iCal.
     * Gera VCALENDAR com todos os eventos do usuario.
     *
     * Regras: Premium only (UC19)
     */
    @RequiresPremium
    @Transactional(readOnly = true)
    public String exportIcal(UUID userId);
}
```

#### AttendanceService

```java
@Service
@Transactional
public class AttendanceService {

    /**
     * Atualizar presenca em evento de static.
     * Fluxo:
     * 1. Verifica que o usuario e membro da static
     * 2. Busca ou cria Attendance
     * 3. Atualiza status (CONFIRMED, TENTATIVE, ABSENT)
     * 4. Se ABSENT: notifica lider (RN29)
     * 5. Notifica membros via WebSocket (/topic/static.{staticId})
     *
     * Regras: RN25 (confirmacao obrigatoria), RN29 (notificar lider se ausente)
     */
    public AttendanceResponse updateAttendance(UUID userId, UUID eventId, UpdateAttendanceRequest request);

    /**
     * Inscrever-se em evento PUG.
     * Fluxo:
     * 1. Verifica que o evento e publico
     * 2. Verifica que o character e verificado
     * 3. Verifica vagas: se currentParticipants < maxParticipants -> REGISTERED
     *    Senao -> WAITLISTED (RN27: lista de espera)
     * 4. Cria Attendance
     */
    public AttendanceResponse registerForPug(UUID userId, UUID eventId);

    /**
     * Cancelar inscricao em PUG.
     * Se alguem esta na WAITLIST, promove para REGISTERED.
     */
    public void unregisterFromPug(UUID userId, UUID eventId);
}
```

#### ReminderScheduler

```java
@Component
public class ReminderScheduler {

    /**
     * Job de lembretes automaticos.
     * Executa a cada 15 minutos.
     *
     * Fluxo:
     * 1. Busca eventos que iniciam em ~24h (janela de 15min)
     *    -> Envia lembrete "24h antes" para membros/participantes
     * 2. Busca eventos que iniciam em ~1h (janela de 15min)
     *    -> Envia lembrete "1h antes" para membros/participantes
     * 3. Para premium: verifica preferencias de lembrete customizado
     * 4. Envia via NotificationService (in-app + push + email para premium)
     * 5. Registra lembretes enviados para evitar duplicacao
     *
     * Regras: RN28 (lembretes 24h e 1h, customizavel para premium)
     */
    @Scheduled(cron = "0 */15 * * * *") // cada 15 minutos
    public void checkReminders();
}
```

#### iCalService

```java
@Service
public class ICalService {

    /**
     * Gera calendario no formato iCal (RFC 5545).
     * Inclui:
     * - VCALENDAR header com timezone
     * - VEVENT para cada evento
     * - RRULE para eventos recorrentes
     * - VALARM para lembretes
     */
    public String generateIcal(List<RaidEvent> events, String calendarName);
}
```

#### Eventos Publicados

| Evento                   | Publicado por                        | Dados                        | Consumido por                            |
| ------------------------ | ------------------------------------ | ---------------------------- | ---------------------------------------- |
| `EventCreatedEvent`      | `EventService.createStaticEvent`     | eventId, staticId            | `NotificationService` (notifica membros) |
| `EventCanceledEvent`     | `EventService.cancelEvent`           | eventId, staticId            | `NotificationService`                    |
| `PugEventCreatedEvent`   | `EventService.createPugEvent`        | eventId, dataCenter          | `NotificationService`                    |
| `AttendanceUpdatedEvent` | `AttendanceService.updateAttendance` | eventId, characterId, status | `NotificationService` (RN29: se ausente) |

#### Eventos Consumidos

| Evento                 | Origem          | Acao                                           |
| ---------------------- | --------------- | ---------------------------------------------- |
| `StaticDissolvedEvent` | `StaticService` | Cancela todos os eventos futuros da static     |
| `MemberJoinedEvent`    | `StaticService` | Cria Attendance TENTATIVE para eventos futuros |
| `MemberLeftEvent`      | `StaticService` | Remove attendances de eventos futuros          |

### 2.5 Controller

#### EventController

```
@RestController
@RequestMapping("/api/v1")
```

| Metodo | Path                           | Descricao                  | Auth                 | Status |
| ------ | ------------------------------ | -------------------------- | -------------------- | ------ |
| GET    | `/statics/{id}/events`         | Eventos da static          | Sim (Member)         | 200    |
| POST   | `/statics/{id}/events`         | Criar evento               | Sim (Leader)         | 201    |
| PUT    | `/events/{eventId}`            | Atualizar evento           | Sim (Leader/Creator) | 200    |
| DELETE | `/events/{eventId}`            | Cancelar evento            | Sim (Leader/Creator) | 204    |
| GET    | `/events/public`               | Eventos PUG                | Nao                  | 200    |
| POST   | `/events/public`               | Criar evento PUG           | Sim (Premium)        | 201    |
| PUT    | `/events/{eventId}/attendance` | Confirmar presenca         | Sim                  | 200    |
| POST   | `/events/{eventId}/register`   | Inscrever-se em PUG        | Sim                  | 201    |
| DELETE | `/events/{eventId}/register`   | Cancelar inscricao PUG     | Sim                  | 204    |
| GET    | `/calendar/me`                 | Meu calendario consolidado | Sim                  | 200    |
| GET    | `/calendar/me/ical`            | Exportar iCal              | Sim (Premium)        | 200    |

**Exemplos de Request/Response:**

```json
// POST /api/v1/statics/{id}/events
// Request:
{
  "title": "P12S Prog Night",
  "description": "Phase 2 prog. Please review the guide beforehand.",
  "contentType": "SAVAGE",
  "encounter": "P12S",
  "startTime": "2026-03-05T01:00:00Z",
  "endTime": "2026-03-05T04:00:00Z",
  "timezone": "America/New_York",
  "isRecurring": true,
  "recurrenceRule": "FREQ=WEEKLY;BYDAY=TU,TH",
  "recurrenceEnd": "2026-06-30T04:00:00Z",
  "isPublic": false
}

// Response (201):
{
  "id": "bb0e8400-e29b-41d4-a716-446655440040",
  "staticId": "880e8400-...",
  "staticName": "Heavens' Gate",
  "title": "P12S Prog Night",
  "description": "Phase 2 prog. Please review the guide beforehand.",
  "contentType": "SAVAGE",
  "encounter": "P12S",
  "startTime": "2026-03-05T01:00:00Z",
  "endTime": "2026-03-05T04:00:00Z",
  "timezone": "America/New_York",
  "isRecurring": true,
  "recurrenceRule": "FREQ=WEEKLY;BYDAY=TU,TH",
  "isPublic": false,
  "maxParticipants": null,
  "currentParticipants": 0,
  "dataCenter": null,
  "createdById": "550e8400-...",
  "isCanceled": false,
  "attendances": [
    {
      "id": "cc0e8400-...",
      "character": { "name": "Luna Starfall", "...": "..." },
      "status": "TENTATIVE",
      "note": null,
      "respondedAt": null
    }
  ],
  "createdAt": "2026-03-04T21:00:00Z"
}
```

```json
// PUT /api/v1/events/{eventId}/attendance
// Request:
{
  "status": "CONFIRMED",
  "note": "Will be 5 min late"
}
```

### 2.6 Seguranca e Autorizacao

```java
// Eventos de static: apenas membros podem ver, apenas lider pode criar/editar/cancelar
// Eventos PUG publicos: qualquer um pode ver, premium pode criar
// Presenca: apenas membros da static ou inscritos em PUG
// iCal: premium only

@PreAuthorize("isAuthenticated()")
// + verificacoes de membership/leadership no service
```

### 2.7 Validacoes

| Campo                 | Regra                                  | Contexto                |
| --------------------- | -------------------------------------- | ----------------------- |
| `title`               | Obrigatorio, max 200 chars             | CreateEventRequest      |
| `startTime`           | Obrigatorio, deve ser no futuro        | CreateEventRequest      |
| `endTime`             | Obrigatorio, deve ser apos startTime   | CreateEventRequest      |
| `timezone`            | Timezone valido (IANA)                 | CreateEventRequest      |
| `recurrenceRule`      | Formato RRULE valido (se isRecurring)  | CreateEventRequest      |
| `maxParticipants`     | Obrigatorio e positivo para PUG        | CreateEventRequest      |
| `dataCenter`          | Obrigatorio para PUG                   | CreateEventRequest      |
| `status` (attendance) | Enum valido                            | UpdateAttendanceRequest |
| PUG vagas             | currentParticipants <= maxParticipants | AttendanceService       |

### 2.8 Cache (Redis)

| Chave                              | Tipo          | TTL   | Descricao                          | Invalidacao                           |
| ---------------------------------- | ------------- | ----- | ---------------------------------- | ------------------------------------- |
| `events:static:{staticId}:{month}` | String (JSON) | 5 min | Cache de eventos do mes por static | Criacao/edicao/cancelamento de evento |
| `events:public:{dc}`               | String (JSON) | 2 min | Cache de eventos PUG por DC        | Criacao de novo PUG                   |
| `calendar:{userId}:{month}`        | String (JSON) | 2 min | Calendario consolidado do usuario  | Qualquer mudanca de evento            |

### 2.9 Jobs Agendados

| Job                 | Frequencia      | Descricao                                                                                                            |
| ------------------- | --------------- | -------------------------------------------------------------------------------------------------------------------- |
| `ReminderScheduler` | Cada 15 minutos | Envia lembretes 24h e 1h antes dos eventos. Verifica preferencias de lembrete premium (RN28). Evita envio duplicado. |

---

## 3. Frontend

### 3.1 Paginas e Rotas

| Rota                                      | Renderizacao | Descricao                         |
| ----------------------------------------- | ------------ | --------------------------------- |
| `/[locale]/(main)/calendar`               | CSR          | Calendario consolidado do usuario |
| `/[locale]/(main)/calendar/events/[id]`   | CSR          | Detalhe do evento com presencas   |
| `/[locale]/(main)/calendar/events/create` | CSR          | Criar evento (static ou PUG)      |
| `/[locale]/(main)/calendar/public`        | CSR          | Eventos PUG publicos              |

### 3.2 Componentes React

```
calendar/page.tsx                    -- Client Component
  CalendarView                       -- FullCalendar wrapper
    MonthView / WeekView / DayView   -- Modos de visualizacao
    EventItem                        -- Item de evento no calendario
      EventTooltip                   -- Tooltip com detalhes
  CalendarSidebar                    -- Sidebar com lista de eventos
    EventListItem                    -- Item na lista lateral
  CalendarFilters                    -- Filtros (por static, tipo, etc.)

calendar/events/[id]/page.tsx        -- Client Component
  EventDetail                        -- Detalhe do evento
    EventHeader                      -- Titulo, data, horario
    EventDescription                 -- Descricao formatada
    EventInfo                        -- Conteudo, encounter, recorrencia
    AttendancePanel                  -- Painel de presencas
      AttendanceList                 -- Lista de presencas
        AttendanceBadge              -- Badge de status (Confirmed, Absent, etc.)
      MyAttendanceForm               -- Formulario de confirmacao
        StatusSelect                 -- Select de status
        NoteInput                    -- Campo de nota
    ParticipantList (PUG)            -- Lista de inscritos no PUG
    WaitlistIndicator (PUG)          -- Indicador de lista de espera

calendar/events/create/page.tsx      -- Client Component
  EventForm                          -- Formulario de criacao
    TitleInput
    DescriptionEditor
    ContentTypeSelect
    EncounterInput
    DateTimePicker                    -- Seletor de data/hora com timezone
    TimezonePicker
    RecurrenceEditor                  -- Editor de recorrencia
      FrequencySelect                -- Semanal, quinzenal
      DaySelector                    -- Dias da semana
      EndDatePicker                  -- Data final da recorrencia
    PugOptions                       -- Opcoes de PUG (se publico)
      MaxParticipantsInput
      DataCenterSelect

calendar/public/page.tsx             -- Client Component
  PugEventList                       -- Lista de eventos PUG
    PugEventCard                     -- Card de evento PUG
      EventDateTime                  -- Data e horario
      ContentInfo                    -- Tipo de conteudo e encounter
      SlotsIndicator                 -- Vagas restantes
      RegisterButton                 -- Botao de inscricao
    PugFilters                       -- Filtros (DC, conteudo, data)
```

### 3.3 Estado e Data Fetching

#### Hooks TanStack Query

```typescript
// hooks/use-calendar.ts

function useStaticEvents(staticId: string, start: string, end: string) {
  return useQuery({
    queryKey: ["events", "static", staticId, start, end],
    queryFn: () => api.get(`/statics/${staticId}/events`, { params: { start, end } }),
  });
}

function useMyCalendar(start: string, end: string) {
  return useQuery({
    queryKey: ["calendar", "me", start, end],
    queryFn: () => api.get("/calendar/me", { params: { start, end } }),
  });
}

function usePublicEvents(dataCenter: string, page: number = 0) {
  return useQuery({
    queryKey: ["events", "public", dataCenter, page],
    queryFn: () => api.get("/events/public", { params: { dataCenter, page } }),
  });
}

function useEvent(eventId: string) {
  return useQuery({
    queryKey: ["events", eventId],
    queryFn: () => api.get(`/events/${eventId}`),
  });
}

function useCreateEvent() {
  return useMutation({
    mutationFn: ({ staticId, data }: { staticId?: string; data: CreateEventRequest }) =>
      staticId ? api.post(`/statics/${staticId}/events`, data) : api.post("/events/public", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
      queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}

function useUpdateAttendance() {
  return useMutation({
    mutationFn: ({ eventId, data }: { eventId: string; data: UpdateAttendanceRequest }) =>
      api.put(`/events/${eventId}/attendance`, data),
    onSuccess: (_, { eventId }) => {
      queryClient.invalidateQueries({ queryKey: ["events", eventId] });
      queryClient.invalidateQueries({ queryKey: ["calendar"] });
    },
  });
}

function useRegisterPug() {
  return useMutation({
    mutationFn: (eventId: string) => api.post(`/events/${eventId}/register`),
    onSuccess: (_, eventId) => {
      queryClient.invalidateQueries({ queryKey: ["events", eventId] });
      queryClient.invalidateQueries({ queryKey: ["events", "public"] });
    },
  });
}

function useExportIcal() {
  return useMutation({
    mutationFn: () => api.get("/calendar/me/ical", { responseType: "blob" }),
    onSuccess: (data) => {
      // Trigger download do arquivo .ics
      const blob = new Blob([data], { type: "text/calendar" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "ffxiv-calendar.ics";
      a.click();
    },
  });
}
```

#### WebSocket (atualizacoes em tempo real)

```typescript
// Atualizacoes de eventos via canal da static
stompClient.subscribe(`/topic/static.${staticId}`, (message) => {
  const data = JSON.parse(message.body);
  if (
    data.type === "EVENT_CREATED" ||
    data.type === "EVENT_UPDATED" ||
    data.type === "ATTENDANCE_UPDATED"
  ) {
    queryClient.invalidateQueries({ queryKey: ["events"] });
    queryClient.invalidateQueries({ queryKey: ["calendar"] });
  }
});
```

### 3.4 Formularios e Validacao (Zod)

```typescript
// schemas/event.ts
import { z } from "zod";

export const createEventSchema = z
  .object({
    title: z.string().min(1, "Title is required").max(200),
    description: z.string().max(2000).optional(),
    contentType: z.enum(["SAVAGE", "ULTIMATE", "EXTREME", "CRITERION"]).optional(),
    encounter: z.string().max(100).optional(),
    startTime: z.string().datetime(),
    endTime: z.string().datetime(),
    timezone: z.string().min(1, "Timezone is required"),
    isRecurring: z.boolean().default(false),
    recurrenceRule: z.string().max(200).optional(),
    recurrenceEnd: z.string().datetime().optional(),
    isPublic: z.boolean().default(false),
    maxParticipants: z.number().int().positive().optional(),
    dataCenter: z.string().optional(),
  })
  .refine((data) => new Date(data.endTime) > new Date(data.startTime), {
    message: "End time must be after start time",
    path: ["endTime"],
  })
  .refine((data) => !data.isPublic || (data.maxParticipants && data.dataCenter), {
    message: "PUG events require max participants and data center",
    path: ["maxParticipants"],
  });

export const updateAttendanceSchema = z.object({
  status: z.enum(["CONFIRMED", "TENTATIVE", "ABSENT"]),
  note: z.string().max(500).optional(),
});
```

### 3.5 WebSocket

| Destination                | Tipo      | Descricao                                     |
| -------------------------- | --------- | --------------------------------------------- |
| `/topic/static.{staticId}` | SUBSCRIBE | Atualizacoes de eventos e presencas da static |

### 3.6 Internacionalizacao

**Namespaces i18n:**

| Namespace             | Uso                      |
| --------------------- | ------------------------ |
| `calendar.*`          | Pagina de calendario     |
| `event.*`             | Criar/editar/ver eventos |
| `event.contentType.*` | Tipos de conteudo        |
| `attendance.*`        | Status de presenca       |
| `attendance.status.*` | Labels de status         |
| `pug.*`               | Eventos PUG              |
| `reminder.*`          | Lembretes                |

---

## 4. Banco de Dados

### 4.1 DDL

```sql
-- Migration: V5__create_calendar_events.sql

CREATE TABLE raid_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    static_id           UUID REFERENCES statics(id) ON DELETE CASCADE,
    title               VARCHAR(200) NOT NULL,
    description         VARCHAR(2000),
    content_type        VARCHAR(50),
    encounter           VARCHAR(100),
    start_time          TIMESTAMPTZ NOT NULL,
    end_time            TIMESTAMPTZ NOT NULL,
    timezone            VARCHAR(50) NOT NULL,
    is_recurring        BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_rule     VARCHAR(200),
    recurrence_end      TIMESTAMPTZ,
    is_public           BOOLEAN NOT NULL DEFAULT FALSE,
    max_participants    INTEGER,
    data_center         VARCHAR(20),
    created_by          UUID NOT NULL REFERENCES users(id),
    canceled_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE attendances (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL REFERENCES raid_events(id) ON DELETE CASCADE,
    character_id    UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'TENTATIVE',
    note            VARCHAR(500),
    responded_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_event_character UNIQUE (event_id, character_id)
);

-- Indices
CREATE INDEX idx_events_static_start ON raid_events(static_id, start_time) WHERE canceled_at IS NULL;
CREATE INDEX idx_events_public ON raid_events(is_public, data_center, start_time) WHERE is_public = TRUE AND canceled_at IS NULL;
CREATE INDEX idx_events_start_time ON raid_events(start_time) WHERE canceled_at IS NULL;
CREATE INDEX idx_events_created_by ON raid_events(created_by);
CREATE INDEX idx_attendances_event ON attendances(event_id);
CREATE INDEX idx_attendances_character ON attendances(character_id);
```

### 4.2 Indices

| Tabela        | Indice                               | Tipo            | Justificativa                     |
| ------------- | ------------------------------------ | --------------- | --------------------------------- |
| `raid_events` | `static_id, start_time`              | BTREE (partial) | Eventos futuros da static         |
| `raid_events` | `is_public, data_center, start_time` | BTREE (partial) | PUGs publicos por DC              |
| `raid_events` | `start_time`                         | BTREE (partial) | Busca de lembretes                |
| `attendances` | `event_id, character_id`             | UNIQUE          | Uma presenca por evento/character |
| `attendances` | `event_id`                           | BTREE           | Presencas do evento               |
| `attendances` | `character_id`                       | BTREE           | Eventos do jogador                |

---

## 5. Integracao Entre Modulos

### 5.1 Dependencias

```
calendar
    |
    +--> auth-profile (Character para attendances)
    |
    +--> static-management (Static para eventos)
    |
    +--> premium (PUG creation, iCal export, lembretes customizados)
    |
    +--> notification (lembretes e notificacoes de eventos)
    |
    <-- static-management (StaticDissolvedEvent, MemberJoinedEvent, MemberLeftEvent)
```

---

## 6. Tratamento de Erros

| HTTP | Tipo RFC 7807                | Cenario                                          |
| ---- | ---------------------------- | ------------------------------------------------ |
| 400  | `/errors/validation`         | Campos invalidos                                 |
| 400  | `/errors/invalid-time-range` | endTime anterior a startTime                     |
| 400  | `/errors/invalid-recurrence` | Regra de recorrencia invalida                    |
| 403  | `/errors/not-leader`         | Nao e lider da static                            |
| 403  | `/errors/not-member`         | Nao e membro da static                           |
| 403  | `/errors/premium-required`   | Criacao de PUG ou exportacao iCal requer premium |
| 404  | `/errors/event-not-found`    | Evento nao encontrado                            |
| 409  | `/errors/event-canceled`     | Evento ja cancelado                              |
| 409  | `/errors/event-full`         | Evento PUG lotado (RN27)                         |
| 409  | `/errors/already-registered` | Ja inscrito no PUG                               |
