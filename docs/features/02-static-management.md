# Feature 02 - Gestao de Statics

## 1. Visao Geral

O modulo de Gestao de Statics e o core do sistema. Ele abrange todo o ciclo de vida de uma static (grupo fixo de raid): criacao, configuracao, recrutamento bidirecional (vagas, candidaturas e convites), gestao de membros, transferencia de lideranca e dissolucao. Este modulo implementa o sistema bidirecional de recrutamento onde statics publicam vagas e jogadores se candidatam, ou statics convidam jogadores diretamente.

### 1.1 Rastreabilidade

| Tipo | IDs |
|------|-----|
| **Regras de Negocio** | RN06, RN07, RN08, RN09, RN10, RN11, RN12, RN13, RN14, RN15, RN16, RN17, RN18 |
| **Casos de Uso** | UC05 (Criar Static), UC06 (Gerenciar Static), UC07 (Publicar Vaga), UC08 (Candidatar-se a Vaga), UC09 (Convidar Jogador), UC10 (Buscar Statics), UC11 (Buscar Jogadores) |

---

## 2. Backend

### 2.1 Entities (JPA)

#### Static

```java
@Entity
@Table(name = "statics")
public class Static extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "content_scope", columnDefinition = "jsonb")
    private String contentScope; // ["SAVAGE", "ULTIMATE", "EXTREME"]

    @Column(name = "loot_rules", length = 500)
    private String lootRules;

    @Column(length = 10)
    private String language; // en, ja, de, fr, pt

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StaticStatus status; // RECRUITING, ACTIVE, INACTIVE, DISSOLVED

    @Column(name = "max_members", nullable = false)
    private int maxMembers = 8;

    @Column(name = "data_center", length = 20)
    private String dataCenter;

    @Column(name = "server", length = 50)
    private String server;

    @Column(name = "schedule", columnDefinition = "jsonb")
    private String schedule; // JSON com dias/horarios de raid

    @Column(name = "deleted_at")
    private Instant deletedAt; // soft delete

    @OneToMany(mappedBy = "static_", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StaticMembership> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "static_", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StaticVacancy> vacancies = new ArrayList<>();

    @OneToMany(mappedBy = "static_", cascade = CascadeType.ALL)
    private List<Invitation> invitations = new ArrayList<>();
}

public enum StaticStatus {
    RECRUITING,  // buscando membros ativamente
    ACTIVE,      // full party, raidando
    INACTIVE,    // pausada temporariamente
    DISSOLVED    // dissolvida permanentemente
}
```

#### StaticMembership

```java
@Entity
@Table(name = "static_memberships")
public class StaticMembership extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "static_id", nullable = false)
    private Static static_;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    private Role role; // TANK, HEALER, DPS

    @Column(name = "job_assigned", length = 3)
    private String jobAssigned; // Job especifico atribuido (PLD, WHM, etc.)

    @Column(name = "is_leader", nullable = false)
    private boolean isLeader = false;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt; // null = membro ativo
}
```

#### StaticVacancy

```java
@Entity
@Table(name = "static_vacancies")
public class StaticVacancy extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "static_id", nullable = false)
    private Static static_;

    @Column(name = "role_needed", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Role roleNeeded; // TANK, HEALER, DPS

    @Column(name = "job_preferences", columnDefinition = "jsonb")
    private String jobPreferences; // ["PLD", "WAR", "DRK", "GNB"]

    @Column(name = "requirements", columnDefinition = "jsonb")
    private String requirements; // {"minIlvl": 660, "minParseTier": "BLUE", "progression": "P12S"}

    @Column(length = 500)
    private String description;

    @Column(name = "is_open", nullable = false)
    private boolean isOpen = true;

    @OneToMany(mappedBy = "vacancy", cascade = CascadeType.ALL)
    private List<Application> applications = new ArrayList<>();
}
```

#### Application

```java
@Entity
@Table(name = "applications")
public class Application extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacancy_id", nullable = false)
    private StaticVacancy vacancy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status; // PENDING, ACCEPTED, REJECTED, EXPIRED, WITHDRAWN

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "response_message", length = 500)
    private String responseMessage;
}

public enum ApplicationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    WITHDRAWN
}
```

#### Invitation

```java
@Entity
@Table(name = "invitations")
public class Invitation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "static_id", nullable = false)
    private Static static_;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InvitationStatus status; // PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELED

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "vacancy_id")
    private UUID vacancyId; // referencia opcional a vaga que o convite preenche
}

public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    CANCELED
}
```

### 2.2 DTOs (Java Records)

#### Request DTOs

```java
// Criar/Atualizar Static
public record CreateStaticRequest(
    @NotBlank @Size(max = 100)
    String name,

    @Size(max = 2000)
    String description,

    List<String> contentScope, // ["SAVAGE", "ULTIMATE"]

    @Size(max = 500)
    String lootRules,

    @Size(max = 10)
    String language,

    @Min(2) @Max(8)
    int maxMembers,

    @NotBlank @Size(max = 20)
    String dataCenter,

    @Size(max = 50)
    String server,

    String schedule // JSON
) {}

// Criar/Atualizar Vaga
public record CreateVacancyRequest(
    @NotNull
    Role roleNeeded,

    List<String> jobPreferences, // ["PLD", "WAR"]

    String requirements, // JSON com criterios

    @Size(max = 500)
    String description
) {}

// Candidatura
public record CreateApplicationRequest(
    @NotNull
    UUID characterId,

    @Size(max = 1000)
    String message
) {}

// Responder candidatura
public record RespondApplicationRequest(
    @Size(max = 500)
    String responseMessage
) {}

// Convite
public record CreateInvitationRequest(
    @NotNull
    UUID characterId,

    @Size(max = 1000)
    String message,

    UUID vacancyId // opcional - vaga que o convite preenche
) {}

// Alterar membro
public record UpdateMemberRequest(
    Role role,
    String jobAssigned
) {}

// Transferir lideranca
public record TransferLeadershipRequest(
    @NotNull
    UUID newLeaderMembershipId
) {}

// Busca de statics
public record SearchStaticsRequest(
    String dataCenter,
    String server,
    Role roleNeeded,
    String language,
    String contentScope, // filtro por tipo de conteudo
    StaticStatus status,
    String query,        // busca textual por nome
    int page,
    int size,
    String sort
) {}

// Busca de jogadores
public record SearchPlayersRequest(
    String dataCenter,
    String server,
    Role role,
    String jobName,
    String language,
    String parseTier,
    boolean lookingForStatic,
    String query,
    int page,
    int size,
    String sort
) {}
```

#### Response DTOs

```java
// Static
public record StaticResponse(
    UUID id,
    String name,
    String description,
    List<String> contentScope,
    String lootRules,
    String language,
    StaticStatus status,
    int maxMembers,
    int currentMembers,
    String dataCenter,
    String server,
    String schedule,
    List<MembershipResponse> members,
    List<VacancyResponse> vacancies,
    Instant createdAt,
    Instant updatedAt
) {}

// Static Summary (para listagens e busca)
public record StaticSummaryResponse(
    UUID id,
    String name,
    List<String> contentScope,
    String language,
    StaticStatus status,
    int maxMembers,
    int currentMembers,
    String dataCenter,
    String server,
    int openVacancies,
    Instant createdAt
) {}

// Membership
public record MembershipResponse(
    UUID id,
    CharacterSummaryResponse character,
    Role role,
    String jobAssigned,
    boolean isLeader,
    Instant joinedAt
) {}

// Vacancy
public record VacancyResponse(
    UUID id,
    Role roleNeeded,
    List<String> jobPreferences,
    String requirements,
    String description,
    boolean isOpen,
    int applicationCount,
    Instant createdAt
) {}

// Application
public record ApplicationResponse(
    UUID id,
    CharacterSummaryResponse character,
    VacancySummaryResponse vacancy,
    String message,
    ApplicationStatus status,
    Instant appliedAt,
    Instant respondedAt,
    String responseMessage
) {}

// Invitation
public record InvitationResponse(
    UUID id,
    StaticSummaryResponse static_,
    CharacterSummaryResponse character,
    String message,
    InvitationStatus status,
    Instant sentAt,
    Instant respondedAt
) {}
```

#### Mapper (MapStruct)

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CharacterMapper.class})
public interface StaticMapper {
    StaticResponse toResponse(Static static_);
    StaticSummaryResponse toSummary(Static static_);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "memberships", ignore = true)
    @Mapping(target = "vacancies", ignore = true)
    @Mapping(target = "invitations", ignore = true)
    @Mapping(target = "status", constant = "RECRUITING")
    Static toEntity(CreateStaticRequest request);
}

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VacancyMapper {
    VacancyResponse toResponse(StaticVacancy vacancy);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "static_", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "isOpen", constant = "true")
    StaticVacancy toEntity(CreateVacancyRequest request);
}

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CharacterMapper.class})
public interface ApplicationMapper {
    ApplicationResponse toResponse(Application application);
}

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CharacterMapper.class, StaticMapper.class})
public interface InvitationMapper {
    InvitationResponse toResponse(Invitation invitation);
}
```

### 2.3 Repository

```java
public interface StaticRepository extends JpaRepository<Static, UUID> {

    @Query("SELECT s FROM Static s WHERE s.deletedAt IS NULL AND s.id = :id")
    Optional<Static> findActiveById(@Param("id") UUID id);

    @Query("SELECT s FROM Static s JOIN s.memberships m " +
           "WHERE m.character.user.id = :userId AND m.leftAt IS NULL AND s.deletedAt IS NULL")
    List<Static> findByMemberUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(s) FROM Static s JOIN s.memberships m " +
           "WHERE m.character.user.id = :userId AND m.isLeader = true AND m.leftAt IS NULL AND s.deletedAt IS NULL")
    long countByLeaderUserId(@Param("userId") UUID userId);

    // Busca com filtros dinamicos via Specification
    Page<Static> findAll(Specification<Static> spec, Pageable pageable);
}

// Specification para busca dinamica
public class StaticSpecifications {

    public static Specification<Static> withFilters(SearchStaticsRequest request) {
        return Specification.where(isActive())
            .and(hasDataCenter(request.dataCenter()))
            .and(hasServer(request.server()))
            .and(hasRoleNeeded(request.roleNeeded()))
            .and(hasLanguage(request.language()))
            .and(hasContentScope(request.contentScope()))
            .and(hasStatus(request.status()))
            .and(nameContains(request.query()));
    }

    private static Specification<Static> isActive() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<Static> hasDataCenter(String dc) {
        return dc == null ? null :
            (root, query, cb) -> cb.equal(root.get("dataCenter"), dc);
    }

    private static Specification<Static> hasRoleNeeded(Role role) {
        if (role == null) return null;
        return (root, query, cb) -> {
            var vacancyJoin = root.join("vacancies");
            return cb.and(
                cb.equal(vacancyJoin.get("roleNeeded"), role),
                cb.isTrue(vacancyJoin.get("isOpen"))
            );
        };
    }
    // ... demais specifications
}

public interface StaticMembershipRepository extends JpaRepository<StaticMembership, UUID> {

    @Query("SELECT m FROM StaticMembership m WHERE m.static_.id = :staticId AND m.leftAt IS NULL")
    List<StaticMembership> findActiveByStaticId(@Param("staticId") UUID staticId);

    Optional<StaticMembership> findByStaticIdAndCharacterId(UUID staticId, UUID characterId);

    @Query("SELECT m FROM StaticMembership m WHERE m.static_.id = :staticId AND m.isLeader = true AND m.leftAt IS NULL")
    Optional<StaticMembership> findLeaderByStaticId(@Param("staticId") UUID staticId);

    @Query("SELECT COUNT(m) FROM StaticMembership m WHERE m.static_.id = :staticId AND m.leftAt IS NULL")
    int countActiveByStaticId(@Param("staticId") UUID staticId);

    @Query("SELECT m FROM StaticMembership m WHERE m.character.id = :charId AND m.leftAt IS NULL")
    List<StaticMembership> findActiveByCharacterId(@Param("charId") UUID characterId);
}

public interface StaticVacancyRepository extends JpaRepository<StaticVacancy, UUID> {

    List<StaticVacancy> findByStaticIdAndIsOpenTrue(UUID staticId);

    @Query("SELECT COUNT(v) FROM StaticVacancy v WHERE v.static_.id = :staticId AND v.isOpen = true")
    int countOpenByStaticId(@Param("staticId") UUID staticId);
}

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @Query("SELECT a FROM Application a WHERE a.vacancy.static_.id = :staticId AND a.status = 'PENDING'")
    List<Application> findPendingByStaticId(@Param("staticId") UUID staticId);

    @Query("SELECT a FROM Application a WHERE a.character.user.id = :userId")
    Page<Application> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    boolean existsByVacancyIdAndCharacterId(UUID vacancyId, UUID characterId);

    @Query("SELECT a FROM Application a WHERE a.status = 'PENDING' AND a.appliedAt < :expirationDate")
    List<Application> findExpired(@Param("expirationDate") Instant expirationDate);
}

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    @Query("SELECT i FROM Invitation i WHERE i.character.user.id = :userId AND i.status = 'PENDING'")
    List<Invitation> findPendingByUserId(@Param("userId") UUID userId);

    boolean existsByStaticIdAndCharacterIdAndStatus(UUID staticId, UUID characterId, InvitationStatus status);

    @Query("SELECT i FROM Invitation i WHERE i.status = 'PENDING' AND i.sentAt < :expirationDate")
    List<Invitation> findExpired(@Param("expirationDate") Instant expirationDate);
}
```

### 2.4 Service

#### StaticService

```java
@Service
@Transactional
public class StaticService {

    /**
     * Criar nova static.
     * Fluxo:
     * 1. Verifica limite de criacao (RN10): free = 1, premium = ilimitado
     * 2. Busca character principal do usuario
     * 3. Valida que character e verificado (RN02 implicitamente)
     * 4. Cria Static com status RECRUITING
     * 5. Cria StaticMembership com isLeader=true para o criador
     * 6. Cria chat room automatico para a static (via evento)
     * 7. Publica StaticCreatedEvent
     *
     * Regras: RN06 (2-8 membros), RN07 (1 lider), RN08 (configs), RN09 (conteudo), RN10 (limite free)
     */
    public StaticResponse createStatic(UUID userId, CreateStaticRequest request);

    /**
     * Obter static por ID (perfil publico).
     */
    @Transactional(readOnly = true)
    public StaticResponse getStatic(UUID staticId);

    /**
     * Atualizar static.
     * Verifica que o usuario e o lider da static (RN07).
     */
    public StaticResponse updateStatic(UUID userId, UUID staticId, CreateStaticRequest request);

    /**
     * Dissolver static (soft delete).
     * Fluxo:
     * 1. Verifica que o usuario e o lider
     * 2. Marca deletedAt = agora
     * 3. Marca leftAt para todos os membros ativos
     * 4. Fecha todas as vagas abertas
     * 5. Cancela todas as candidaturas e convites pendentes
     * 6. Notifica todos os membros (RN12)
     * 7. Publica StaticDissolvedEvent
     *
     * Regras: RN12 (notificacao e preservacao de historico)
     */
    public void dissolveStatic(UUID userId, UUID staticId);

    /**
     * Listar membros ativos de uma static.
     */
    @Transactional(readOnly = true)
    public List<MembershipResponse> getMembers(UUID staticId);

    /**
     * Alterar membro (role/job assignment).
     * Apenas lider pode alterar.
     */
    public MembershipResponse updateMember(UUID userId, UUID staticId, UUID membershipId, UpdateMemberRequest request);

    /**
     * Remover membro (kick).
     * Fluxo:
     * 1. Verifica que o usuario e o lider
     * 2. Verifica que nao esta removendo a si mesmo (deve usar leaveStatic)
     * 3. Marca leftAt no membership
     * 4. Notifica o membro removido
     * 5. Publica MemberLeftEvent
     */
    public void removeMember(UUID userId, UUID staticId, UUID membershipId);

    /**
     * Transferir lideranca.
     * Fluxo:
     * 1. Verifica que o usuario e o lider atual
     * 2. Busca novo lider pelo membershipId
     * 3. Atualiza isLeader: false no lider atual, true no novo lider
     * 4. Notifica ambos
     * 5. Publica LeadershipTransferredEvent
     *
     * Regras: RN07 (lideranca transferivel)
     */
    public void transferLeadership(UUID userId, UUID staticId, TransferLeadershipRequest request);

    /**
     * Sair da static voluntariamente.
     * Fluxo:
     * 1. Se e lider, deve transferir lideranca antes (ou static e dissolvida se ultimo membro)
     * 2. Marca leftAt no membership
     * 3. Notifica lider
     * 4. Publica MemberLeftEvent
     */
    public void leaveStatic(UUID userId, UUID staticId);

    /**
     * Listar minhas statics (como membro).
     */
    @Transactional(readOnly = true)
    public List<StaticSummaryResponse> getMyStatics(UUID userId);
}
```

#### VacancyService

```java
@Service
@Transactional
public class VacancyService {

    /**
     * Criar vaga na static.
     * Fluxo:
     * 1. Verifica que o usuario e o lider
     * 2. Verifica que a static tem espaco (membros ativos < maxMembers)
     * 3. Cria StaticVacancy com isOpen=true
     * 4. Publica VacancyCreatedEvent
     *
     * Regras: RN13 (vagas com requisitos)
     */
    public VacancyResponse createVacancy(UUID userId, UUID staticId, CreateVacancyRequest request);

    /**
     * Atualizar vaga.
     */
    public VacancyResponse updateVacancy(UUID userId, UUID staticId, UUID vacancyId, CreateVacancyRequest request);

    /**
     * Fechar vaga.
     */
    public void closeVacancy(UUID userId, UUID staticId, UUID vacancyId);

    /**
     * Listar vagas de uma static.
     */
    @Transactional(readOnly = true)
    public List<VacancyResponse> getVacancies(UUID staticId);
}
```

#### ApplicationService

```java
@Service
@Transactional
public class ApplicationService {

    /**
     * Candidatar-se a uma vaga.
     * Fluxo:
     * 1. Verifica que a vaga esta aberta
     * 2. Verifica que o character e verificado
     * 3. Verifica que nao ja se candidatou a mesma vaga
     * 4. Verifica que nao e membro ativo da static
     * 5. Cria Application com status PENDING
     * 6. Notifica lider da static
     * 7. Publica ApplicationCreatedEvent
     *
     * Regras: RN14 (jogadores podem se candidatar), RN17 (status PENDING)
     */
    public ApplicationResponse apply(UUID userId, UUID vacancyId, CreateApplicationRequest request);

    /**
     * Aceitar candidatura.
     * Fluxo:
     * 1. Verifica que o usuario e o lider da static
     * 2. Verifica que ha espaco na static (membros < maxMembers)
     * 3. Atualiza status para ACCEPTED
     * 4. Cria StaticMembership para o candidato
     * 5. Fecha a vaga automaticamente (isOpen=false)
     * 6. Rejeita automaticamente as demais candidaturas da mesma vaga
     * 7. Notifica o candidato
     * 8. Publica ApplicationAcceptedEvent, MemberJoinedEvent
     */
    public ApplicationResponse acceptApplication(UUID userId, UUID applicationId, RespondApplicationRequest request);

    /**
     * Recusar candidatura.
     * Fluxo:
     * 1. Verifica que o usuario e o lider
     * 2. Atualiza status para REJECTED
     * 3. Notifica o candidato
     */
    public ApplicationResponse rejectApplication(UUID userId, UUID applicationId, RespondApplicationRequest request);

    /**
     * Cancelar candidatura (pelo candidato).
     * Atualiza status para WITHDRAWN.
     */
    public void withdrawApplication(UUID userId, UUID applicationId);

    /**
     * Listar candidaturas de uma static (para o lider).
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getStaticApplications(UUID userId, UUID staticId);

    /**
     * Listar minhas candidaturas.
     */
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(UUID userId, Pageable pageable);
}
```

#### InvitationService

```java
@Service
@Transactional
public class InvitationService {

    /**
     * Convidar jogador para a static.
     * Fluxo:
     * 1. Verifica que o usuario e o lider
     * 2. Verifica que o character alvo e verificado
     * 3. Verifica que nao e membro ativo da static
     * 4. Verifica que nao existe convite pendente para o mesmo character
     * 5. Verifica que o jogador nao bloqueou a static/lider
     * 6. Cria Invitation com status PENDING
     * 7. Notifica o jogador convidado
     * 8. Publica InvitationCreatedEvent
     *
     * Regras: RN15 (statics podem convidar), RN18 (status PENDING)
     */
    public InvitationResponse invite(UUID userId, UUID staticId, CreateInvitationRequest request);

    /**
     * Aceitar convite.
     * Fluxo:
     * 1. Verifica que o convite e para o usuario autenticado
     * 2. Verifica que ha espaco na static
     * 3. Atualiza status para ACCEPTED
     * 4. Cria StaticMembership
     * 5. Se vacancyId informado, fecha a vaga
     * 6. Notifica lider
     * 7. Publica InvitationAcceptedEvent, MemberJoinedEvent
     */
    public InvitationResponse acceptInvitation(UUID userId, UUID invitationId);

    /**
     * Recusar convite.
     */
    public InvitationResponse rejectInvitation(UUID userId, UUID invitationId);

    /**
     * Cancelar convite (pelo lider).
     */
    public void cancelInvitation(UUID userId, UUID invitationId);

    /**
     * Listar convites recebidos pelo usuario.
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyInvitations(UUID userId);
}
```

#### SearchService

```java
@Service
@Transactional(readOnly = true)
public class SearchService {

    /**
     * Buscar statics com filtros.
     * Usa Specifications para filtros dinamicos.
     * Filtros: dataCenter, server, roleNeeded, language, contentScope, status, query (nome).
     *
     * Regras: RN16 (statics publicam vagas), RN13 (requisitos)
     */
    public Page<StaticSummaryResponse> searchStatics(SearchStaticsRequest request);

    /**
     * Buscar jogadores com filtros.
     * Filtros: dataCenter, server, role, jobName, language, parseTier, lookingForStatic.
     * Apenas jogadores com character verificado aparecem.
     *
     * Regras: RN16 (jogadores publicam perfil LFS)
     */
    public Page<CharacterSummaryResponse> searchPlayers(SearchPlayersRequest request);
}
```

#### Eventos Publicados

| Evento | Publicado por | Dados | Consumido por |
|--------|--------------|-------|---------------|
| `StaticCreatedEvent` | `StaticService.createStatic` | staticId, leaderUserId | `ChatService` (cria chat room), `NotificationService` |
| `StaticDissolvedEvent` | `StaticService.dissolveStatic` | staticId, memberUserIds | `ChatService` (desativa chat), `NotificationService` (notifica membros), `CalendarService` (cancela eventos futuros) |
| `MemberJoinedEvent` | `ApplicationService` / `InvitationService` | staticId, characterId, userId | `ChatService` (adiciona ao chat), `NotificationService`, `CalendarService` (sincroniza calendario) |
| `MemberLeftEvent` | `StaticService.removeMember/leave` | staticId, characterId, userId | `ChatService` (remove do chat), `NotificationService` |
| `ApplicationCreatedEvent` | `ApplicationService.apply` | applicationId, staticId | `NotificationService` (notifica lider) |
| `ApplicationAcceptedEvent` | `ApplicationService.accept` | applicationId, characterId | `NotificationService` (notifica candidato) |
| `InvitationCreatedEvent` | `InvitationService.invite` | invitationId, characterId | `NotificationService` (notifica jogador) |
| `InvitationAcceptedEvent` | `InvitationService.accept` | invitationId, staticId | `NotificationService` (notifica lider) |
| `VacancyCreatedEvent` | `VacancyService.create` | vacancyId, staticId | `MatchingService` (recalcular matches) |
| `LeadershipTransferredEvent` | `StaticService.transferLeadership` | staticId, oldLeaderId, newLeaderId | `NotificationService` |

### 2.5 Controller

#### StaticController

```
@RestController
@RequestMapping("/api/v1/statics")
```

| Metodo | Path | Descricao | Auth | Status |
|--------|------|-----------|------|--------|
| POST | `/` | Criar static | Sim | 201 |
| GET | `/{id}` | Ver static | Nao | 200 |
| PUT | `/{id}` | Atualizar static | Sim (Leader) | 200 |
| DELETE | `/{id}` | Dissolver static | Sim (Leader) | 204 |
| GET | `/{id}/members` | Listar membros | Nao | 200 |
| PUT | `/{id}/members/{memberId}` | Alterar membro | Sim (Leader) | 200 |
| DELETE | `/{id}/members/{memberId}` | Remover membro | Sim (Leader) | 204 |
| POST | `/{id}/transfer-leadership` | Transferir lideranca | Sim (Leader) | 200 |
| POST | `/{id}/leave` | Sair da static | Sim (Member) | 204 |

#### VacancyController

```
@RestController
@RequestMapping("/api/v1/statics/{staticId}/vacancies")
```

| Metodo | Path | Descricao | Auth | Status |
|--------|------|-----------|------|--------|
| POST | `/` | Criar vaga | Sim (Leader) | 201 |
| PUT | `/{vacId}` | Atualizar vaga | Sim (Leader) | 200 |
| DELETE | `/{vacId}` | Fechar vaga | Sim (Leader) | 204 |
| GET | `/` | Listar vagas | Nao | 200 |

#### ApplicationController

```
@RestController
@RequestMapping("/api/v1")
```

| Metodo | Path | Descricao | Auth | Status |
|--------|------|-----------|------|--------|
| POST | `/vacancies/{vacId}/applications` | Candidatar-se | Sim | 201 |
| GET | `/statics/{id}/applications` | Listar candidaturas | Sim (Leader) | 200 |
| GET | `/profile/me/applications` | Minhas candidaturas | Sim | 200 |
| PUT | `/applications/{appId}/accept` | Aceitar candidatura | Sim (Leader) | 200 |
| PUT | `/applications/{appId}/reject` | Recusar candidatura | Sim (Leader) | 200 |
| DELETE | `/applications/{appId}` | Cancelar candidatura | Sim (Applicant) | 204 |

#### InvitationController

```
@RestController
@RequestMapping("/api/v1")
```

| Metodo | Path | Descricao | Auth | Status |
|--------|------|-----------|------|--------|
| POST | `/statics/{id}/invitations` | Convidar jogador | Sim (Leader) | 201 |
| GET | `/profile/me/invitations` | Meus convites | Sim | 200 |
| PUT | `/invitations/{invId}/accept` | Aceitar convite | Sim | 200 |
| PUT | `/invitations/{invId}/reject` | Recusar convite | Sim | 200 |
| DELETE | `/invitations/{invId}` | Cancelar convite | Sim (Leader) | 204 |

#### SearchController

```
@RestController
@RequestMapping("/api/v1/search")
```

| Metodo | Path | Descricao | Auth | Status |
|--------|------|-----------|------|--------|
| GET | `/statics` | Buscar statics | Sim | 200 |
| GET | `/players` | Buscar jogadores | Sim | 200 |

**Exemplos de Request/Response:**

```json
// POST /api/v1/statics
// Request:
{
  "name": "Heavens' Gate",
  "description": "Midcore static looking for dedicated players. We raid twice a week.",
  "contentScope": ["SAVAGE"],
  "lootRules": "Need/Greed, 1 item per week",
  "language": "en",
  "maxMembers": 8,
  "dataCenter": "Primal",
  "server": "Lamia",
  "schedule": "{\"tuesday\": \"20:00-23:00 EST\", \"thursday\": \"20:00-23:00 EST\"}"
}

// Response (201):
{
  "id": "880e8400-e29b-41d4-a716-446655440020",
  "name": "Heavens' Gate",
  "description": "Midcore static looking for dedicated players. We raid twice a week.",
  "contentScope": ["SAVAGE"],
  "lootRules": "Need/Greed, 1 item per week",
  "language": "en",
  "status": "RECRUITING",
  "maxMembers": 8,
  "currentMembers": 1,
  "dataCenter": "Primal",
  "server": "Lamia",
  "schedule": "{...}",
  "members": [
    {
      "id": "990e8400-...",
      "character": {
        "id": "660e8400-...",
        "name": "Luna Starfall",
        "server": "Lamia",
        "dataCenter": "Primal",
        "isVerified": true,
        "avatarUrl": "https://...",
        "mainJobName": "WHM"
      },
      "role": "HEALER",
      "jobAssigned": "WHM",
      "isLeader": true,
      "joinedAt": "2026-03-04T20:00:00Z"
    }
  ],
  "vacancies": [],
  "createdAt": "2026-03-04T20:00:00Z",
  "updatedAt": "2026-03-04T20:00:00Z"
}
```

### 2.6 Seguranca e Autorizacao

```java
// Verificacao de lideranca no Service
private void verifyLeader(UUID userId, UUID staticId) {
    StaticMembership membership = membershipRepository
        .findByStaticIdAndCharacterId(staticId, getMainCharacterId(userId))
        .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

    if (!membership.isLeader()) {
        throw new AccessDeniedException("Only the static leader can perform this action");
    }
}

// @PreAuthorize no Controller
@PreAuthorize("isAuthenticated()")
@PutMapping("/{id}")
public StaticResponse updateStatic(@PathVariable UUID id, @RequestBody @Valid CreateStaticRequest request) {
    return staticService.updateStatic(getCurrentUserId(), id, request);
}
```

### 2.7 Validacoes

| Campo | Regra | Contexto |
|-------|-------|----------|
| `name` (static) | Obrigatorio, max 100 chars, unico por lider | CreateStaticRequest |
| `maxMembers` | Entre 2 e 8 (RN06) | CreateStaticRequest |
| `contentScope` | Lista de valores validos (SAVAGE, ULTIMATE, EXTREME, CRITERION) | CreateStaticRequest |
| `roleNeeded` | Enum TANK/HEALER/DPS | CreateVacancyRequest |
| `message` (application) | Max 1000 chars | CreateApplicationRequest |
| `characterId` | Deve ser verificado, deve pertencer ao usuario | CreateApplicationRequest |
| Candidatura duplicada | Nao pode se candidatar a mesma vaga duas vezes | ApplicationService |
| Auto-candidatura | Nao pode se candidatar a propria static | ApplicationService |
| Espaco na static | currentMembers < maxMembers para aceitar/convidar | ApplicationService, InvitationService |

### 2.8 Cache (Redis)

| Chave | Tipo | TTL | Descricao | Invalidacao |
|-------|------|-----|-----------|-------------|
| `static:{staticId}` | String (JSON) | 5 min | Cache do perfil publico da static | Qualquer atualizacao na static |
| `static:members:{staticId}` | String (JSON) | 2 min | Cache dos membros da static | Membro adicionado/removido |

### 2.9 Jobs Agendados

| Job | Frequencia | Descricao |
|-----|-----------|-----------|
| `ExpirationJob` | Diario (00:00 UTC) | Expira candidaturas com mais de 14 dias sem resposta (RN17). Expira convites com mais de 7 dias sem resposta (RN18). Atualiza status para EXPIRED e notifica os envolvidos. |

---

## 3. Frontend

### 3.1 Paginas e Rotas

| Rota | Renderizacao | Descricao |
|------|-------------|-----------|
| `/[locale]/(main)/statics` | CSR | Listar minhas statics (como membro/lider) |
| `/[locale]/(main)/statics/create` | CSR | Criar nova static |
| `/[locale]/(main)/statics/[id]` | SSR + ISR (60s) | Pagina publica da static |
| `/[locale]/(main)/statics/[id]/manage` | CSR | Gerenciar static (lider) |
| `/[locale]/(main)/statics/[id]/applications` | CSR | Candidaturas recebidas (lider) |
| `/[locale]/(main)/search/statics` | CSR | Buscar statics com filtros |
| `/[locale]/(main)/search/players` | CSR | Buscar jogadores com filtros |

### 3.2 Componentes React

```
statics/page.tsx                    -- Client Component
  MyStaticsList                     -- Lista de statics do usuario
    StaticCard                      -- Card resumido da static
      StatusBadge                   -- Badge de status (RECRUITING, ACTIVE, etc.)
      MemberAvatars                 -- Avatares dos membros
      ContentScopeTags              -- Tags de conteudo (SAVAGE, ULTIMATE)

statics/create/page.tsx             -- Client Component
  CreateStaticForm                  -- Formulario de criacao
    NameInput
    DescriptionEditor               -- Editor de texto rico
    ContentScopeSelect              -- Multi-select de conteudo
    LootRulesInput
    LanguageSelect
    MaxMembersSlider
    DataCenterSelect
    ServerSelect
    ScheduleEditor                  -- Editor de horarios

statics/[id]/page.tsx               -- Server Component (ISR)
  StaticProfile                     -- Perfil publico da static
    StaticHeader                    -- Nome, banner, status
    StaticDescription               -- Descricao formatada
    MemberList                      -- Lista de membros com roles
      MemberCard                    -- Card do membro
    VacancyList                     -- Vagas abertas
      VacancyCard                   -- Card da vaga com requisitos
        ApplyButton                 -- Botao de candidatura
    StaticInfo                      -- Info lateral (DC, server, horarios, loot)

statics/[id]/manage/page.tsx        -- Client Component
  StaticManagePanel                 -- Painel de gestao (lider)
    MemberManagement                -- Gerenciar membros
      MemberRow                     -- Linha de membro com acoes
        RoleSelect, JobSelect
        KickButton, TransferButton
    VacancyManagement               -- Gerenciar vagas
      VacancyForm                   -- Criar/editar vaga
    StaticSettings                  -- Configuracoes da static
    DissolveButton                  -- Dissolver static (com confirmacao)

statics/[id]/applications/page.tsx  -- Client Component
  ApplicationList                   -- Lista de candidaturas
    ApplicationCard                 -- Card do candidato
      CharacterPreview              -- Preview do personagem
      ApplicationMessage            -- Mensagem do candidato
      AcceptButton, RejectButton

search/statics/page.tsx             -- Client Component
  StaticSearchPage
    SearchFilters                   -- Filtros (DataCenter, Role, Language, etc.)
    StaticResultList                -- Lista de resultados
      StaticCard                    -- Card resumido (reutilizado)
    Pagination                      -- Paginacao

search/players/page.tsx             -- Client Component
  PlayerSearchPage
    SearchFilters                   -- Filtros (DataCenter, Role, Job, Parse)
    PlayerResultList                -- Lista de resultados
      PlayerCard                    -- Card do jogador
        InviteButton                -- Botao de convidar (se lider)
    Pagination
```

### 3.3 Estado e Data Fetching

#### Hooks TanStack Query

```typescript
// hooks/use-statics.ts
function useMyStatics() {
  return useQuery({
    queryKey: ['statics', 'mine'],
    queryFn: () => api.get('/statics/mine'),
  });
}

function useStatic(id: string) {
  return useQuery({
    queryKey: ['statics', id],
    queryFn: () => api.get(`/statics/${id}`),
    staleTime: 60_000,
  });
}

function useCreateStatic() {
  return useMutation({
    mutationFn: (data: CreateStaticRequest) => api.post('/statics', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['statics', 'mine'] });
    },
  });
}

function useDissolveStatic() {
  return useMutation({
    mutationFn: (id: string) => api.delete(`/statics/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['statics'] });
    },
  });
}

// hooks/use-applications.ts
function useApply() {
  return useMutation({
    mutationFn: ({ vacancyId, data }: { vacancyId: string; data: CreateApplicationRequest }) =>
      api.post(`/vacancies/${vacancyId}/applications`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications', 'mine'] });
    },
  });
}

function useMyApplications(page: number = 0) {
  return useQuery({
    queryKey: ['applications', 'mine', page],
    queryFn: () => api.get(`/profile/me/applications?page=${page}`),
  });
}

function useAcceptApplication() {
  return useMutation({
    mutationFn: ({ appId, data }: { appId: string; data?: RespondApplicationRequest }) =>
      api.put(`/applications/${appId}/accept`, data),
    onSuccess: (_, { appId }) => {
      queryClient.invalidateQueries({ queryKey: ['statics'] });
      queryClient.invalidateQueries({ queryKey: ['applications'] });
    },
  });
}

// hooks/use-invitations.ts
function useMyInvitations() {
  return useQuery({
    queryKey: ['invitations', 'mine'],
    queryFn: () => api.get('/profile/me/invitations'),
  });
}

function useInvitePlayer() {
  return useMutation({
    mutationFn: ({ staticId, data }: { staticId: string; data: CreateInvitationRequest }) =>
      api.post(`/statics/${staticId}/invitations`, data),
  });
}

// hooks/use-search.ts (URL state com nuqs)
function useSearchStatics() {
  const [filters] = useQueryStates({
    dataCenter: parseAsString,
    role: parseAsString,
    language: parseAsString,
    contentScope: parseAsString,
    query: parseAsString,
    page: parseAsInteger.withDefault(0),
  });

  return useQuery({
    queryKey: ['search', 'statics', filters],
    queryFn: () => api.get('/search/statics', { params: filters }),
  });
}

function useSearchPlayers() {
  const [filters] = useQueryStates({
    dataCenter: parseAsString,
    role: parseAsString,
    jobName: parseAsString,
    lookingForStatic: parseAsBoolean,
    page: parseAsInteger.withDefault(0),
  });

  return useQuery({
    queryKey: ['search', 'players', filters],
    queryFn: () => api.get('/search/players', { params: filters }),
  });
}
```

### 3.4 Formularios e Validacao (Zod)

```typescript
// schemas/static.ts
import { z } from 'zod';

export const createStaticSchema = z.object({
  name: z.string().min(1, 'Static name is required').max(100),
  description: z.string().max(2000).optional(),
  contentScope: z.array(z.enum(['SAVAGE', 'ULTIMATE', 'EXTREME', 'CRITERION'])).min(1, 'Select at least one content type'),
  lootRules: z.string().max(500).optional(),
  language: z.enum(['en', 'ja', 'de', 'fr', 'pt']),
  maxMembers: z.number().int().min(2).max(8).default(8),
  dataCenter: z.string().min(1, 'Data Center is required'),
  server: z.string().optional(),
  schedule: z.any().optional(),
});

// schemas/vacancy.ts
export const createVacancySchema = z.object({
  roleNeeded: z.enum(['TANK', 'HEALER', 'DPS']),
  jobPreferences: z.array(z.string().max(3)).optional(),
  requirements: z.any().optional(),
  description: z.string().max(500).optional(),
});

// schemas/application.ts
export const createApplicationSchema = z.object({
  characterId: z.string().uuid(),
  message: z.string().max(1000).optional(),
});

// schemas/invitation.ts
export const createInvitationSchema = z.object({
  characterId: z.string().uuid(),
  message: z.string().max(1000).optional(),
  vacancyId: z.string().uuid().optional(),
});
```

### 3.5 WebSocket

Atualizacoes de static (novos membros, candidaturas) sao enviadas via:

- `/topic/static.{staticId}` -- atualizacoes da static em tempo real
- `/user/queue/notifications` -- notificacoes pessoais (convites, aceites)

### 3.6 Internacionalizacao

**Namespaces i18n:**

| Namespace | Uso |
|-----------|-----|
| `static.*` | Paginas de static (criar, ver, gerenciar) |
| `static.status.*` | Labels de status |
| `vacancy.*` | Vagas e requisitos |
| `application.*` | Candidaturas |
| `invitation.*` | Convites |
| `search.*` | Paginas de busca |
| `search.filters.*` | Labels de filtros |
| `member.*` | Gestao de membros |
| `ffxiv.contentScope.*` | Tipos de conteudo (Savage, Ultimate, etc.) |
| `ffxiv.lootRules.*` | Regras de loot comuns |

---

## 4. Banco de Dados

### 4.1 DDL

```sql
-- Migration: V2__create_statics_and_memberships.sql

CREATE TABLE statics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(2000),
    content_scope   JSONB, -- ["SAVAGE", "ULTIMATE"]
    loot_rules      VARCHAR(500),
    language        VARCHAR(10),
    status          VARCHAR(20) NOT NULL DEFAULT 'RECRUITING',
    max_members     INTEGER NOT NULL DEFAULT 8,
    data_center     VARCHAR(20),
    server          VARCHAR(50),
    schedule        JSONB,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE static_memberships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    static_id       UUID NOT NULL REFERENCES statics(id) ON DELETE CASCADE,
    character_id    UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    role            VARCHAR(10),
    job_assigned    VARCHAR(3),
    is_leader       BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_static_character UNIQUE (static_id, character_id)
);

-- Migration: V3__create_vacancies_applications_invitations.sql

CREATE TABLE static_vacancies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    static_id       UUID NOT NULL REFERENCES statics(id) ON DELETE CASCADE,
    role_needed     VARCHAR(10) NOT NULL,
    job_preferences JSONB,
    requirements    JSONB,
    description     VARCHAR(500),
    is_open         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE applications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vacancy_id      UUID NOT NULL REFERENCES static_vacancies(id) ON DELETE CASCADE,
    character_id    UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    message         VARCHAR(1000),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at    TIMESTAMPTZ,
    response_message VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    static_id       UUID NOT NULL REFERENCES statics(id) ON DELETE CASCADE,
    character_id    UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    message         VARCHAR(1000),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at    TIMESTAMPTZ,
    vacancy_id      UUID REFERENCES static_vacancies(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indices
CREATE INDEX idx_statics_status_dc ON statics(status, data_center) WHERE deleted_at IS NULL;
CREATE INDEX idx_statics_name_trgm ON statics USING gin (name gin_trgm_ops) WHERE deleted_at IS NULL;
CREATE INDEX idx_memberships_static_id ON static_memberships(static_id) WHERE left_at IS NULL;
CREATE INDEX idx_memberships_character_id ON static_memberships(character_id) WHERE left_at IS NULL;
CREATE INDEX idx_vacancies_static_id ON static_vacancies(static_id, is_open);
CREATE INDEX idx_applications_vacancy_status ON applications(vacancy_id, status);
CREATE INDEX idx_applications_character_status ON applications(character_id, status);
CREATE INDEX idx_applications_applied_at ON applications(applied_at) WHERE status = 'PENDING';
CREATE INDEX idx_invitations_character_status ON invitations(character_id, status);
CREATE INDEX idx_invitations_static_status ON invitations(static_id, status);
CREATE INDEX idx_invitations_sent_at ON invitations(sent_at) WHERE status = 'PENDING';
```

### 4.2 Indices

| Tabela | Indice | Tipo | Justificativa |
|--------|--------|------|---------------|
| `statics` | `status, data_center` | BTREE (partial) | Busca de statics ativas por DC |
| `statics` | `name` | GIN (trigram) | Busca textual por nome |
| `static_memberships` | `static_id, character_id` | UNIQUE | Evitar membro duplicado |
| `static_memberships` | `static_id` (partial) | BTREE | Membros ativos da static |
| `static_memberships` | `character_id` (partial) | BTREE | Statics ativas do jogador |
| `static_vacancies` | `static_id, is_open` | BTREE | Vagas abertas por static |
| `applications` | `vacancy_id, status` | BTREE | Candidaturas pendentes |
| `applications` | `character_id, status` | BTREE | Minhas candidaturas |
| `applications` | `applied_at` (partial) | BTREE | Expirar candidaturas antigas |
| `invitations` | `character_id, status` | BTREE | Convites recebidos |
| `invitations` | `sent_at` (partial) | BTREE | Expirar convites antigos |

---

## 5. Integracao Entre Modulos

### 5.1 Dependencias

```
static-management
    |
    +--> auth-profile (Character para memberships)
    |
    +--> premium (verifica limite de criacao - RN10)
    |
    +--> notification (notifica sobre candidaturas, convites, dissolucao)
    |
    <-- matching (consulta vagas abertas, perfis LFS)
    |
    <-- calendar (cria eventos vinculados a statics)
    |
    <-- reputation (reviews vinculadas a statics e membros)
    |
    <-- chat (chat room automatico por static)
```

### 5.2 Eventos Publicados e Consumidos

| Direcao | Evento | Consumidor | Acao |
|---------|--------|-----------|------|
| Saida | `StaticCreatedEvent` | `ChatService` | Cria chat room da static |
| Saida | `StaticDissolvedEvent` | `ChatService`, `CalendarService` | Desativa chat, cancela eventos |
| Saida | `MemberJoinedEvent` | `ChatService`, `ReputationService` | Adiciona ao chat, inicia timer de coexistencia |
| Saida | `MemberLeftEvent` | `ChatService` | Remove do chat room |
| Saida | `VacancyCreatedEvent` | `MatchingService` | Recalcula sugestoes de match |
| Entrada | `SubscriptionExpiredEvent` | `StaticService` | Statics extras em read-only (RN44) |

---

## 6. Tratamento de Erros

| HTTP | Tipo RFC 7807 | Cenario |
|------|---------------|---------|
| 400 | `/errors/validation` | Campos invalidos |
| 400 | `/errors/invalid-max-members` | maxMembers fora de 2-8 (RN06) |
| 403 | `/errors/not-leader` | Usuario nao e lider da static |
| 403 | `/errors/premium-required` | Limite de criacao de statics atingido (RN10) |
| 403 | `/errors/character-not-verified` | Character nao verificado |
| 404 | `/errors/static-not-found` | Static nao encontrada |
| 404 | `/errors/vacancy-not-found` | Vaga nao encontrada |
| 404 | `/errors/application-not-found` | Candidatura nao encontrada |
| 404 | `/errors/invitation-not-found` | Convite nao encontrado |
| 404 | `/errors/membership-not-found` | Membro nao encontrado |
| 409 | `/errors/already-applied` | Ja se candidatou a esta vaga |
| 409 | `/errors/already-member` | Ja e membro da static |
| 409 | `/errors/already-invited` | Convite pendente ja existe |
| 409 | `/errors/vacancy-closed` | Vaga ja esta fechada |
| 409 | `/errors/static-full` | Static ja tem o maximo de membros (RN06) |
| 409 | `/errors/cannot-remove-self` | Lider nao pode se remover (deve transferir ou sair) |
| 409 | `/errors/leader-cannot-leave` | Lider deve transferir lideranca antes de sair |
| 422 | `/errors/application-not-pending` | Candidatura nao esta mais pendente |
| 422 | `/errors/invitation-not-pending` | Convite nao esta mais pendente |
