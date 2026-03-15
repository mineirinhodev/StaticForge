# Feature 01 - Autenticacao & Perfil

## 1. Visao Geral

O modulo de Autenticacao & Perfil e a base de todo o sistema. Ele gerencia o ciclo de vida da conta do usuario (registro, login, recuperacao de senha), a autenticacao via JWT e OAuth 2.0, e o gerenciamento de perfis de jogador com vinculo ao Lodestone.

### 1.1 Rastreabilidade

| Tipo                  | IDs                                                                                                                          |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| **Regras de Negocio** | RN01, RN02, RN03, RN04, RN05                                                                                                 |
| **Casos de Uso**      | UC01 (Registrar Conta), UC02 (Criar Perfil de Jogador), UC03 (Vincular Personagem ao Lodestone), UC04 (Login e Autenticacao) |

---

## 2. Backend

### 2.1 Entities (JPA)

#### User

```java
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash; // nullable para usuarios OAuth-only

    @Column(length = 10)
    private String locale; // en, ja, de, fr, pt

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium = false;

    @Column(name = "premium_expires_at")
    private Instant premiumExpiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false; // ativado apos verificacao de email

    @Column(name = "oauth_provider", length = 20)
    private String oauthProvider; // GOOGLE, DISCORD ou null

    @Column(name = "oauth_provider_id", length = 255)
    private String oauthProviderId;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "deleted_at")
    private Instant deletedAt; // soft delete

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameCharacter> characters = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens = new ArrayList<>();
}
```

#### Character

```java
@Entity
@Table(name = "characters")
public class GameCharacter extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String server;

    @Column(name = "data_center", nullable = false, length = 20)
    private String dataCenter;

    @Column(name = "lodestone_id", unique = true)
    private Long lodestoneId;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "is_main", nullable = false)
    private boolean isMain = false;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 20)
    private String race;

    @Column(length = 20)
    private String gender;

    @Column(name = "free_company", length = 100)
    private String freeCompany;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "looking_for_static", nullable = false)
    private boolean lookingForStatic = false;

    @Column(name = "availability", columnDefinition = "jsonb")
    private String availability; // JSON com horarios disponiveis por dia da semana

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterJob> jobs = new ArrayList<>();
}
```

#### CharacterJob

```java
@Entity
@Table(name = "character_jobs")
public class CharacterJob extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(name = "job_name", nullable = false, length = 3)
    private String jobName; // PLD, WAR, WHM, SCH, SAM, etc.

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Role role; // TANK, HEALER, DPS

    @Column(name = "is_main", nullable = false)
    private boolean isMain = false;

    @Column
    private Integer ilvl;

    @Column(name = "parse_tier", length = 20)
    private String parseTier; // GRAY, GREEN, BLUE, PURPLE, ORANGE, PINK

    @Column(length = 100)
    private String progression; // ex: "P12S Phase 2", "DSR Clear"
}
```

#### RefreshToken

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hash do token

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by", length = 64)
    private String replacedBy; // hash do token que substituiu este (rotacao)

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
```

#### AuditableEntity (MappedSuperclass)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

### 2.2 DTOs (Java Records)

#### Request DTOs

```java
// Registro
public record RegisterRequest(
    @NotBlank @Email @Size(max = 255)
    String email,

    @NotBlank @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must contain at least 1 uppercase letter and 1 number")
    String password,

    @NotBlank
    String confirmPassword,

    @Size(max = 10)
    String locale // opcional, default "en"
) {}

// Login
public record LoginRequest(
    @NotBlank @Email
    String email,

    @NotBlank
    String password
) {}

// Refresh Token
public record TokenRefreshRequest(
    // refresh token vem via httpOnly cookie, nao no body
) {}

// Forgot Password
public record ForgotPasswordRequest(
    @NotBlank @Email
    String email
) {}

// Reset Password
public record ResetPasswordRequest(
    @NotBlank
    String token,

    @NotBlank @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$")
    String newPassword,

    @NotBlank
    String confirmPassword
) {}

// Atualizar Perfil
public record ProfileUpdateRequest(
    @Size(max = 10)
    String locale,

    @Size(max = 1000)
    String bio,

    String availability // JSON de disponibilidade
) {}

// Criar/Atualizar Personagem
public record CharacterCreateRequest(
    @NotBlank @Size(max = 50)
    String name,

    @NotBlank @Size(max = 50)
    String server,

    @NotBlank @Size(max = 20)
    String dataCenter,

    boolean isMain,

    boolean lookingForStatic,

    String availability // JSON de disponibilidade
) {}

// Verificacao Lodestone
public record VerificationConfirmRequest(
    @NotNull
    Long lodestoneId
) {}

// Criar/Atualizar Job
public record JobCreateRequest(
    @NotBlank @Size(max = 3)
    String jobName,

    @NotNull
    Role role,

    boolean isMain,

    Integer ilvl,

    @Size(max = 20)
    String parseTier,

    @Size(max = 100)
    String progression
) {}
```

#### Response DTOs

```java
// Login Response
public record LoginResponse(
    String accessToken,
    String tokenType, // "Bearer"
    long expiresIn,   // 900 (15min em segundos)
    UserSummaryResponse user
) {}

// Token Refresh Response
public record TokenRefreshResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}

// User Summary (incluido no login)
public record UserSummaryResponse(
    UUID id,
    String email,
    String locale,
    boolean isPremium,
    CharacterSummaryResponse mainCharacter
) {}

// Perfil Completo
public record ProfileResponse(
    UUID id,
    String email,
    String locale,
    boolean isPremium,
    Instant premiumExpiresAt,
    List<CharacterResponse> characters,
    Instant createdAt
) {}

// Personagem
public record CharacterResponse(
    UUID id,
    String name,
    String server,
    String dataCenter,
    Long lodestoneId,
    boolean isVerified,
    boolean isMain,
    String avatarUrl,
    String race,
    String gender,
    String freeCompany,
    String bio,
    boolean lookingForStatic,
    String availability,
    List<JobResponse> jobs,
    Instant createdAt,
    Instant updatedAt
) {}

// Character Summary (para uso em listagens)
public record CharacterSummaryResponse(
    UUID id,
    String name,
    String server,
    String dataCenter,
    boolean isVerified,
    String avatarUrl,
    String mainJobName
) {}

// Job
public record JobResponse(
    UUID id,
    String jobName,
    Role role,
    boolean isMain,
    Integer ilvl,
    String parseTier,
    String progression
) {}

// Verificacao Lodestone
public record VerificationStartResponse(
    String verificationCode,  // codigo a ser adicionado na bio do Lodestone
    String lodestoneUrl,      // URL do perfil no Lodestone
    String instructions       // instrucoes para o usuario
) {}
```

#### Mapper (MapStruct)

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserSummaryResponse toSummary(User user, CharacterSummaryResponse mainCharacter);
    ProfileResponse toProfile(User user);
}

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CharacterMapper {
    CharacterResponse toResponse(Character character);
    CharacterSummaryResponse toSummary(Character character);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "jobs", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    Character toEntity(CharacterCreateRequest request);
}

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobMapper {
    JobResponse toResponse(CharacterJob job);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "character", ignore = true)
    CharacterJob toEntity(JobCreateRequest request);
}
```

### 2.3 Repository

```java
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.oauthProvider = :provider AND u.oauthProviderId = :providerId")
    Optional<User> findByOAuthProvider(@Param("provider") String provider,
                                       @Param("providerId") String providerId);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.id = :id")
    Optional<User> findActiveById(@Param("id") UUID id);
}

public interface CharacterRepository extends JpaRepository<Character, UUID> {

    List<GameCharacter> findByUserIdOrderByIsMainDesc(UUID userId);

    Optional<Character> findByLodestoneId(Long lodestoneId);

    boolean existsByLodestoneId(Long lodestoneId);

    @Query("SELECT c FROM Character c WHERE c.user.id = :userId AND c.isMain = true")
    Optional<Character> findMainByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Character c JOIN c.jobs j " +
           "WHERE c.dataCenter = :dataCenter AND c.lookingForStatic = true AND c.isVerified = true " +
           "AND c.user.deletedAt IS NULL")
    Page<Character> findLFSByDataCenter(@Param("dataCenter") String dataCenter, Pageable pageable);
}

public interface CharacterJobRepository extends JpaRepository<CharacterJob, UUID> {

    List<CharacterJob> findByCharacterId(UUID characterId);

    @Query("SELECT j FROM CharacterJob j WHERE j.character.id = :charId AND j.isMain = true")
    Optional<CharacterJob> findMainByCharacterId(@Param("charId") UUID characterId);

    boolean existsByCharacterIdAndJobName(UUID characterId, String jobName);
}

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = CURRENT_TIMESTAMP WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpired();
}
```

### 2.4 Service

#### AuthService

```java
@Service
@Transactional
public class AuthService {

    // Dependencias: UserRepository, RefreshTokenRepository, PasswordEncoder,
    //               JwtTokenProvider, ApplicationEventPublisher, EmailService

    /**
     * Registro de nova conta.
     * Fluxo:
     * 1. Valida que email nao esta em uso
     * 2. Valida que senha e confirmacao coincidem
     * 3. Cria User com passwordHash (BCrypt) e isActive=false
     * 4. Gera token de verificacao de email (UUID, expira 24h)
     * 5. Publica UserRegisteredEvent (EmailService escuta e envia email)
     * 6. Retorna mensagem de sucesso
     *
     * Regras: RN01 (indireta - conta base para vincular personagens)
     */
    public void register(RegisterRequest request);

    /**
     * Login com email/senha.
     * Fluxo:
     * 1. Busca usuario por email
     * 2. Verifica se conta esta ativa (email verificado)
     * 3. Verifica lockout (RN - 5 tentativas = 15min lock)
     * 4. Valida senha com BCrypt
     * 5. Se falha: incrementa failedLoginAttempts, se >= 5 seta lockedUntil
     * 6. Se sucesso: reseta failedLoginAttempts
     * 7. Gera access token JWT (15min)
     * 8. Gera refresh token (30 dias), armazena hash no DB
     * 9. Seta refresh token como httpOnly cookie
     * 10. Retorna LoginResponse com accessToken e dados do usuario
     */
    public LoginResponse login(LoginRequest request, HttpServletResponse response);

    /**
     * Renovacao de token.
     * Fluxo:
     * 1. Extrai refresh token do cookie httpOnly
     * 2. Calcula hash SHA-256 e busca no DB
     * 3. Valida que nao esta expirado nem revogado
     * 4. Revoga o token atual (refresh token rotation)
     * 5. Gera novo refresh token e armazena hash
     * 6. Gera novo access token
     * 7. Seta novo refresh token como cookie
     * 8. Retorna TokenRefreshResponse
     */
    public TokenRefreshResponse refresh(HttpServletRequest request, HttpServletResponse response);

    /**
     * Logout.
     * Fluxo:
     * 1. Extrai refresh token do cookie
     * 2. Revoga o refresh token no DB
     * 3. Remove cookie do response
     * 4. Remove sessao do Redis
     */
    public void logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * Solicitar reset de senha.
     * Fluxo:
     * 1. Busca usuario por email (silencioso se nao encontrado - seguranca)
     * 2. Gera token de reset (UUID, expira 1h)
     * 3. Armazena hash no Redis com TTL 1h
     * 4. Publica PasswordResetRequestedEvent (envia email)
     */
    public void forgotPassword(ForgotPasswordRequest request);

    /**
     * Redefinir senha.
     * Fluxo:
     * 1. Valida token de reset no Redis
     * 2. Valida que nova senha e confirmacao coincidem
     * 3. Atualiza passwordHash com BCrypt
     * 4. Revoga todos os refresh tokens do usuario
     * 5. Remove token de reset do Redis
     */
    public void resetPassword(ResetPasswordRequest request);
}
```

#### ProfileService

```java
@Service
@Transactional(readOnly = true)
public class ProfileService {

    /**
     * Obter perfil do usuario autenticado.
     * Retorna ProfileResponse com todos os personagens e jobs.
     */
    public ProfileResponse getMyProfile(UUID userId);

    /**
     * Obter perfil publico de outro usuario.
     * Retorna apenas dados publicos (sem email, sem dados sensiveis).
     * Cache: profile:{characterId} TTL 5min
     *
     * Regras: RN03 (perfil deve ter job, disponibilidade e DC)
     */
    @Cacheable(value = "profiles", key = "#userId")
    public ProfileResponse getPublicProfile(UUID userId);

    /**
     * Atualizar perfil.
     * Fluxo:
     * 1. Busca usuario por ID
     * 2. Atualiza campos permitidos (locale, bio, availability)
     * 3. Invalida cache do perfil
     * 4. Publica ProfileUpdatedEvent
     *
     * Regras: RN05 (idioma afeta interface e matching)
     */
    @Transactional
    @CacheEvict(value = "profiles", key = "#userId")
    public ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request);
}
```

#### CharacterService

```java
@Service
@Transactional
public class CharacterService {

    /**
     * Adicionar personagem.
     * Fluxo:
     * 1. Valida que o usuario existe
     * 2. Se isMain=true, desmarca o main atual (se houver)
     * 3. Se e o primeiro personagem, marca como main automaticamente
     * 4. Cria o Character vinculado ao User
     * 5. Publica CharacterCreatedEvent
     *
     * Regras: RN01 (multiplos personagens, um principal)
     */
    public CharacterResponse createCharacter(UUID userId, CharacterCreateRequest request);

    /**
     * Atualizar personagem.
     * Verifica ownership (userId do character == userId autenticado).
     */
    public CharacterResponse updateCharacter(UUID userId, UUID characterId, CharacterCreateRequest request);

    /**
     * Remover personagem.
     * Fluxo:
     * 1. Verifica ownership
     * 2. Verifica que nao e o unico personagem do usuario (deve ter ao menos 1)
     * 3. Se era main, promove outro personagem a main
     * 4. Remove personagem (cascade remove jobs)
     *
     * Regras: RN01 (deve manter ao menos um personagem)
     */
    public void deleteCharacter(UUID userId, UUID characterId);

    /**
     * Iniciar verificacao Lodestone.
     * Fluxo:
     * 1. Verifica ownership
     * 2. Verifica que lodestoneId nao esta vinculado a outra conta (RN04)
     * 3. Busca perfil no Lodestone via XIVAPI/LodestoneClient
     * 4. Valida que nome e server correspondem ao character
     * 5. Gera codigo de verificacao unico (8 chars alfanumerico)
     * 6. Armazena codigo no Redis com TTL 1h: lodestone:verify:{characterId}
     * 7. Retorna VerificationStartResponse com codigo e instrucoes
     *
     * Regras: RN02 (verificacao obrigatoria para matching/reputacao), RN04 (personagem unico por conta)
     */
    public VerificationStartResponse startVerification(UUID userId, UUID characterId);

    /**
     * Confirmar verificacao Lodestone.
     * Fluxo:
     * 1. Busca codigo de verificacao do Redis
     * 2. Busca perfil no Lodestone via XIVAPI
     * 3. Verifica que o codigo esta presente na bio do Lodestone
     * 4. Se confirmado: marca isVerified=true, importa dados (avatar, race, FC)
     * 5. Armazena dados importados no Redis: lodestone:{lodestoneId} TTL 1h
     * 6. Remove codigo de verificacao do Redis
     * 7. Publica CharacterVerifiedEvent
     *
     * Regras: RN02 (ativa matching e reputacao)
     */
    public CharacterResponse confirmVerification(UUID userId, UUID characterId, VerificationConfirmRequest request);
}
```

#### CharacterJobService

```java
@Service
@Transactional
public class CharacterJobService {

    /**
     * Adicionar job a um personagem.
     * Fluxo:
     * 1. Verifica ownership do personagem
     * 2. Verifica que o job nao esta duplicado no personagem
     * 3. Se isMain=true, desmarca main atual
     * 4. Se e o primeiro job, marca como main automaticamente
     * 5. Cria CharacterJob
     *
     * Regras: RN03 (perfil deve ter ao menos 1 job)
     */
    public JobResponse addJob(UUID userId, UUID characterId, JobCreateRequest request);

    /**
     * Atualizar job.
     */
    public JobResponse updateJob(UUID userId, UUID characterId, UUID jobId, JobCreateRequest request);

    /**
     * Remover job.
     * Verifica que nao e o unico job do personagem.
     */
    public void deleteJob(UUID userId, UUID characterId, UUID jobId);
}
```

#### Eventos Publicados

| Evento                        | Publicado por                          | Dados                     | Consumido por                                                                  |
| ----------------------------- | -------------------------------------- | ------------------------- | ------------------------------------------------------------------------------ |
| `UserRegisteredEvent`         | `AuthService.register`                 | userId, email, locale     | `NotificationService` (email de verificacao)                                   |
| `PasswordResetRequestedEvent` | `AuthService.forgotPassword`           | userId, email, resetToken | `NotificationService` (email de reset)                                         |
| `ProfileUpdatedEvent`         | `ProfileService.updateProfile`         | userId                    | `MatchingService` (recalcular matches)                                         |
| `CharacterCreatedEvent`       | `CharacterService.createCharacter`     | characterId, userId       | -                                                                              |
| `CharacterVerifiedEvent`      | `CharacterService.confirmVerification` | characterId, userId       | `MatchingService` (ativar matching), `NotificationService` (notificar usuario) |

### 2.5 Controller

#### AuthController

```
@RestController
@RequestMapping("/api/v1/auth")
```

| Metodo | Path               | Descricao       | Request Body            | Response Body          | Status |
| ------ | ------------------ | --------------- | ----------------------- | ---------------------- | ------ |
| POST   | `/register`        | Registrar conta | `RegisterRequest`       | `{ "message": "..." }` | 201    |
| POST   | `/login`           | Login           | `LoginRequest`          | `LoginResponse`        | 200    |
| POST   | `/refresh`         | Renovar token   | - (cookie)              | `TokenRefreshResponse` | 200    |
| POST   | `/logout`          | Logout          | -                       | -                      | 204    |
| POST   | `/forgot-password` | Solicitar reset | `ForgotPasswordRequest` | `{ "message": "..." }` | 200    |
| POST   | `/reset-password`  | Redefinir senha | `ResetPasswordRequest`  | `{ "message": "..." }` | 200    |

**Exemplos de Request/Response:**

```json
// POST /api/v1/auth/register
// Request:
{
  "email": "luna@example.com",
  "password": "MyPass123",
  "confirmPassword": "MyPass123",
  "locale": "en"
}

// Response (201):
{
  "message": "Registration successful. Please check your email for verification."
}
```

```json
// POST /api/v1/auth/login
// Request:
{
  "email": "luna@example.com",
  "password": "MyPass123"
}

// Response (200):
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "luna@example.com",
    "locale": "en",
    "isPremium": false,
    "mainCharacter": {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "name": "Luna Starfall",
      "server": "Lamia",
      "dataCenter": "Primal",
      "isVerified": true,
      "avatarUrl": "https://img.finalfantasyxiv.com/...",
      "mainJobName": "WHM"
    }
  }
}
```

#### ProfileController

```
@RestController
@RequestMapping("/api/v1")
```

| Metodo | Path                  | Descricao           | Auth | Response Body     | Status |
| ------ | --------------------- | ------------------- | ---- | ----------------- | ------ |
| GET    | `/profile/me`         | Meu perfil completo | Sim  | `ProfileResponse` | 200    |
| PUT    | `/profile/me`         | Atualizar perfil    | Sim  | `ProfileResponse` | 200    |
| GET    | `/users/{id}/profile` | Perfil publico      | Nao  | `ProfileResponse` | 200    |

#### CharacterController

```
@RestController
@RequestMapping("/api/v1/characters")
```

| Metodo | Path                   | Descricao             | Auth | Request Body                 | Response Body               | Status |
| ------ | ---------------------- | --------------------- | ---- | ---------------------------- | --------------------------- | ------ |
| POST   | `/`                    | Adicionar personagem  | Sim  | `CharacterCreateRequest`     | `CharacterResponse`         | 201    |
| PUT    | `/{id}`                | Atualizar personagem  | Sim  | `CharacterCreateRequest`     | `CharacterResponse`         | 200    |
| DELETE | `/{id}`                | Remover personagem    | Sim  | -                            | -                           | 204    |
| POST   | `/{id}/verify`         | Iniciar verificacao   | Sim  | -                            | `VerificationStartResponse` | 200    |
| POST   | `/{id}/verify/confirm` | Confirmar verificacao | Sim  | `VerificationConfirmRequest` | `CharacterResponse`         | 200    |
| POST   | `/{id}/jobs`           | Adicionar job         | Sim  | `JobCreateRequest`           | `JobResponse`               | 201    |
| PUT    | `/{id}/jobs/{jobId}`   | Atualizar job         | Sim  | `JobCreateRequest`           | `JobResponse`               | 200    |
| DELETE | `/{id}/jobs/{jobId}`   | Remover job           | Sim  | -                            | -                           | 204    |

### 2.6 Seguranca e Autorizacao

#### Endpoints Publicos

```java
// SecurityConfig.java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password",
        "/api/v1/users/*/profile"
    ).permitAll()
    .anyRequest().authenticated()
);
```

#### JWT Components

```java
// JwtProperties.java - Configuracao
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,          // HS256 secret key (min 256 bits)
    long accessExpiration,  // 900000 (15min em ms)
    long refreshExpiration  // 2592000000 (30 dias em ms)
) {}

// JwtTokenProvider.java - Geracao e validacao de tokens
@Component
public class JwtTokenProvider {
    String generateAccessToken(UUID userId, String email);
    String generateRefreshToken();
    UUID getUserIdFromToken(String token);
    boolean validateToken(String token);
}

// JwtAuthenticationFilter.java - Filtro Spring Security
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Extrai token do header Authorization: Bearer {token}
    // Valida com JwtTokenProvider
    // Seta SecurityContextHolder com UserAuthentication
}
```

#### OAuth 2.0

```java
// OAuth2UserService.java
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    // Processa login via Google/Discord
    // Cria User se nao existe, vincula se ja existe
    // Providers: GOOGLE, DISCORD
}

// OAuth2SuccessHandler.java
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    // Apos OAuth bem-sucedido:
    // 1. Gera JWT access token
    // 2. Gera refresh token
    // 3. Redireciona para frontend com token em URL fragment
}
```

#### Rate Limiting (Login)

```
- Chave Redis: rate:login:{email}
- Limite: 5 tentativas em 15 minutos
- Apos 5 falhas: conta bloqueada por 15 minutos (lockedUntil)
- Reset automatico apos login bem-sucedido
```

### 2.7 Validacoes

| Campo              | Regra                                         | DTO                                   |
| ------------------ | --------------------------------------------- | ------------------------------------- |
| `email`            | Formato valido, unico no sistema              | RegisterRequest, LoginRequest         |
| `password`         | Min 8 chars, 1 maiuscula, 1 numero            | RegisterRequest, ResetPasswordRequest |
| `confirmPassword`  | Igual a password                              | RegisterRequest, ResetPasswordRequest |
| `locale`           | Enum: en, ja, de, fr, pt                      | RegisterRequest, ProfileUpdateRequest |
| `name` (character) | Obrigatorio, max 50 chars                     | CharacterCreateRequest                |
| `server`           | Obrigatorio, deve ser server valido do FFXIV  | CharacterCreateRequest                |
| `dataCenter`       | Obrigatorio, deve ser DC valido do FFXIV      | CharacterCreateRequest                |
| `jobName`          | Obrigatorio, max 3 chars, deve ser job valido | JobCreateRequest                      |
| `role`             | Obrigatorio, enum TANK/HEALER/DPS             | JobCreateRequest                      |
| `ilvl`             | Positivo, max 999                             | JobCreateRequest                      |
| `parseTier`        | Enum: GRAY, GREEN, BLUE, PURPLE, ORANGE, PINK | JobCreateRequest                      |
| `lodestoneId`      | Positivo, unico no sistema (RN04)             | VerificationConfirmRequest            |

### 2.8 Cache (Redis)

| Chave                            | Tipo          | TTL      | Descricao                       | Invalidacao              |
| -------------------------------- | ------------- | -------- | ------------------------------- | ------------------------ |
| `session:{userId}`               | Hash          | 30 dias  | Hash do refresh token ativo     | Logout, refresh rotation |
| `profile:{userId}`               | String (JSON) | 5 min    | Cache de perfil publico         | PUT /profile/me          |
| `lodestone:{lodestoneId}`        | String (JSON) | 1 hora   | Dados importados do Lodestone   | Resincronizacao semanal  |
| `lodestone:verify:{characterId}` | String        | 1 hora   | Codigo de verificacao pendente  | Confirmacao ou expiracao |
| `rate:login:{email}`             | Counter       | 15 min   | Contador de tentativas de login | Login bem-sucedido       |
| `email:verify:{token}`           | String        | 24 horas | Token de verificacao de email   | Verificacao ou expiracao |
| `password:reset:{tokenHash}`     | String        | 1 hora   | Token de reset de senha         | Reset ou expiracao       |

### 2.9 Jobs Agendados

| Job                    | Frequencia                  | Descricao                                                                                                                                              |
| ---------------------- | --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `LodestoneRefreshJob`  | Semanal (domingo 05:00 UTC) | Resincroniza dados do Lodestone (avatar, FC, etc.) para todos os personagens verificados. Invalida cache `lodestone:{id}`. Respeita rate limit da API. |
| `CleanupJob` (parcial) | Semanal                     | Remove refresh tokens expirados da tabela `refresh_tokens`.                                                                                            |

---

## 3. Frontend

### 3.1 Paginas e Rotas

| Rota                                  | Renderizacao    | Descricao                                     |
| ------------------------------------- | --------------- | --------------------------------------------- |
| `/[locale]/(auth)/login`              | SSR             | Pagina de login com formulario e botoes OAuth |
| `/[locale]/(auth)/register`           | SSR             | Pagina de registro                            |
| `/[locale]/(auth)/forgot-password`    | SSR             | Solicitar reset de senha                      |
| `/[locale]/(auth)/reset-password`     | SSR             | Redefinir senha (com token na URL)            |
| `/[locale]/(main)/profile`            | CSR             | Meu perfil (dados do usuario autenticado)     |
| `/[locale]/(main)/profile/edit`       | CSR             | Editar perfil                                 |
| `/[locale]/(main)/profile/characters` | CSR             | Gerenciar personagens e jobs                  |
| `/[locale]/users/[id]`                | SSR + ISR (60s) | Perfil publico de outro jogador               |

### 3.2 Componentes React

```
(auth)/login/page.tsx             -- Server Component (shell)
  LoginForm.tsx                   -- Client Component
    EmailInput, PasswordInput
    OAuthButtons (Google, Discord)

(auth)/register/page.tsx          -- Server Component (shell)
  RegisterForm.tsx                -- Client Component
    EmailInput, PasswordInput, ConfirmPasswordInput
    LocaleSelect

(main)/profile/page.tsx           -- Client Component
  ProfileHeader                   -- Avatar, nome, badges
  CharacterList                   -- Lista de personagens
    CharacterCard                 -- Card de personagem com jobs
      JobBadge                    -- Badge do job com icone
  ReputationSummary               -- Score de reputacao resumido

(main)/profile/edit/page.tsx      -- Client Component
  ProfileEditForm                 -- Formulario de edicao
    AvatarUpload
    LocaleSelect
    BioEditor
    AvailabilityGrid              -- Grade de horarios por dia

(main)/profile/characters/page.tsx -- Client Component
  CharacterManager                -- CRUD de personagens
    CharacterForm                 -- Formulario de personagem
    JobManager                    -- CRUD de jobs
      JobForm                     -- Formulario de job
    LodestoneVerification         -- Fluxo de verificacao
      VerificationCodeDisplay     -- Mostra codigo
      VerificationConfirmButton   -- Confirma verificacao
```

### 3.3 Estado e Data Fetching

#### Zustand Store (auth-store.ts)

```typescript
interface AuthState {
  accessToken: string | null;
  user: UserSummary | null;
  isAuthenticated: boolean;

  setAuth: (token: string, user: UserSummary) => void;
  clearAuth: () => void;
  setAccessToken: (token: string) => void;
}
```

#### Hooks TanStack Query

```typescript
// useAuth.ts
function useLogin() {
  return useMutation({
    mutationFn: (data: LoginRequest) => api.post("/auth/login", data),
    onSuccess: (response) => {
      authStore.setAuth(response.accessToken, response.user);
    },
  });
}

function useRegister() {
  return useMutation({
    mutationFn: (data: RegisterRequest) => api.post("/auth/register", data),
  });
}

function useLogout() {
  return useMutation({
    mutationFn: () => api.post("/auth/logout"),
    onSuccess: () => {
      authStore.clearAuth();
      router.push("/login");
    },
  });
}

function useRefreshToken() {
  return useMutation({
    mutationFn: () => api.post("/auth/refresh"),
    onSuccess: (response) => {
      authStore.setAccessToken(response.accessToken);
    },
  });
}

// useProfile.ts
function useMyProfile() {
  return useQuery({
    queryKey: ["profile", "me"],
    queryFn: () => api.get("/profile/me"),
    enabled: authStore.isAuthenticated,
  });
}

function usePublicProfile(userId: string) {
  return useQuery({
    queryKey: ["profile", userId],
    queryFn: () => api.get(`/users/${userId}/profile`),
    staleTime: 60_000, // 1 minuto
  });
}

function useUpdateProfile() {
  return useMutation({
    mutationFn: (data: ProfileUpdateRequest) => api.put("/profile/me", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profile", "me"] });
    },
  });
}

// useCharacters.ts
function useCharacters() {
  return useQuery({
    queryKey: ["characters"],
    queryFn: () => api.get("/profile/me").then((p) => p.characters),
    enabled: authStore.isAuthenticated,
  });
}

function useCreateCharacter() {
  return useMutation({
    mutationFn: (data: CharacterCreateRequest) => api.post("/characters", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profile", "me"] });
      queryClient.invalidateQueries({ queryKey: ["characters"] });
    },
  });
}

function useDeleteCharacter() {
  return useMutation({
    mutationFn: (id: string) => api.delete(`/characters/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profile", "me"] });
      queryClient.invalidateQueries({ queryKey: ["characters"] });
    },
  });
}

function useStartVerification() {
  return useMutation({
    mutationFn: (characterId: string) => api.post(`/characters/${characterId}/verify`),
  });
}

function useConfirmVerification() {
  return useMutation({
    mutationFn: ({ characterId, lodestoneId }: { characterId: string; lodestoneId: number }) =>
      api.post(`/characters/${characterId}/verify/confirm`, { lodestoneId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["profile", "me"] });
      queryClient.invalidateQueries({ queryKey: ["characters"] });
    },
  });
}
```

#### Axios Interceptors

```typescript
// lib/api.ts
const api = axios.create({ baseURL: process.env.NEXT_PUBLIC_API_URL });

// Request interceptor: injeta access token
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: refresh automatico em 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      try {
        const { data } = await axios.post(`${baseURL}/auth/refresh`, null, {
          withCredentials: true,
        });
        useAuthStore.getState().setAccessToken(data.accessToken);
        error.config.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(error.config);
      } catch {
        useAuthStore.getState().clearAuth();
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  },
);
```

### 3.4 Formularios e Validacao (Zod)

```typescript
// schemas/auth.ts
import { z } from "zod";

export const registerSchema = z
  .object({
    email: z.string().email("Invalid email address").max(255),
    password: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .regex(/[A-Z]/, "Password must contain at least 1 uppercase letter")
      .regex(/\d/, "Password must contain at least 1 number"),
    confirmPassword: z.string(),
    locale: z.enum(["en", "ja", "de", "fr", "pt"]).optional().default("en"),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

export const loginSchema = z.object({
  email: z.string().email("Invalid email address"),
  password: z.string().min(1, "Password is required"),
});

export const forgotPasswordSchema = z.object({
  email: z.string().email("Invalid email address"),
});

export const resetPasswordSchema = z
  .object({
    token: z.string().min(1),
    newPassword: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .regex(/[A-Z]/, "Password must contain at least 1 uppercase letter")
      .regex(/\d/, "Password must contain at least 1 number"),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

// schemas/profile.ts
export const profileSchema = z.object({
  locale: z.enum(["en", "ja", "de", "fr", "pt"]).optional(),
  bio: z.string().max(1000).optional(),
  availability: z
    .record(
      z.array(
        z.object({
          start: z.string(), // "20:00"
          end: z.string(), // "23:00"
          timezone: z.string(),
        }),
      ),
    )
    .optional(),
});

// schemas/character.ts
export const characterSchema = z.object({
  name: z.string().min(1, "Character name is required").max(50),
  server: z.string().min(1, "Server is required").max(50),
  dataCenter: z.string().min(1, "Data Center is required").max(20),
  isMain: z.boolean().default(false),
  lookingForStatic: z.boolean().default(false),
  availability: z.any().optional(), // JSON
});

// schemas/job.ts
export const jobSchema = z.object({
  jobName: z.string().min(1).max(3),
  role: z.enum(["TANK", "HEALER", "DPS"]),
  isMain: z.boolean().default(false),
  ilvl: z.number().int().positive().max(999).optional(),
  parseTier: z.enum(["GRAY", "GREEN", "BLUE", "PURPLE", "ORANGE", "PINK"]).optional(),
  progression: z.string().max(100).optional(),
});
```

### 3.5 WebSocket

Nao aplicavel diretamente a este modulo. A autenticacao WebSocket e tratada no modulo de Chat (Feature 06).

### 3.6 Internacionalizacao

**Namespaces i18n:**

| Namespace                  | Uso                                             |
| -------------------------- | ----------------------------------------------- |
| `auth.login.*`             | Pagina de login (titulo, labels, botoes, erros) |
| `auth.register.*`          | Pagina de registro                              |
| `auth.forgotPassword.*`    | Pagina de recuperacao de senha                  |
| `auth.resetPassword.*`     | Pagina de redefinicao de senha                  |
| `profile.*`                | Paginas de perfil                               |
| `character.*`              | Gerenciamento de personagens                    |
| `character.verification.*` | Fluxo de verificacao Lodestone                  |
| `job.*`                    | Gerenciamento de jobs                           |
| `validation.*`             | Mensagens de erro de validacao                  |
| `ffxiv.jobs.*`             | Nomes oficiais dos jobs por idioma              |
| `ffxiv.servers.*`          | Nomes de servers e Data Centers                 |
| `ffxiv.roles.*`            | Tank, Healer, DPS traduzidos                    |

**Exemplo de chaves:**

```json
{
  "auth": {
    "login": {
      "title": "Login",
      "emailLabel": "Email address",
      "passwordLabel": "Password",
      "submitButton": "Sign in",
      "forgotPassword": "Forgot your password?",
      "registerLink": "Don't have an account? Register",
      "oauthGoogle": "Sign in with Google",
      "oauthDiscord": "Sign in with Discord",
      "errors": {
        "invalidCredentials": "Invalid email or password",
        "accountLocked": "Account locked. Try again in {minutes} minutes.",
        "accountNotVerified": "Please verify your email address"
      }
    }
  }
}
```

---

## 4. Banco de Dados

### 4.1 DDL

```sql
-- Migration: V1__create_users_and_characters.sql

-- Tabela de usuarios
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    locale          VARCHAR(10) DEFAULT 'en',
    is_premium      BOOLEAN NOT NULL DEFAULT FALSE,
    premium_expires_at TIMESTAMPTZ,
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    oauth_provider  VARCHAR(20),
    oauth_provider_id VARCHAR(255),
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabela de personagens
CREATE TABLE characters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(50) NOT NULL,
    server          VARCHAR(50) NOT NULL,
    data_center     VARCHAR(20) NOT NULL,
    lodestone_id    BIGINT UNIQUE,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    is_main         BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_url      VARCHAR(500),
    race            VARCHAR(20),
    gender          VARCHAR(20),
    free_company    VARCHAR(100),
    bio             VARCHAR(1000),
    looking_for_static BOOLEAN NOT NULL DEFAULT FALSE,
    availability    JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabela de jobs do personagem
CREATE TABLE character_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id    UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    job_name        VARCHAR(3) NOT NULL,
    role            VARCHAR(10) NOT NULL, -- TANK, HEALER, DPS
    is_main         BOOLEAN NOT NULL DEFAULT FALSE,
    ilvl            INTEGER,
    parse_tier      VARCHAR(20),
    progression     VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_character_job UNIQUE (character_id, job_name)
);

-- Tabela de refresh tokens
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(64) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    replaced_by     VARCHAR(64),
    user_agent      VARCHAR(500),
    ip_address      VARCHAR(45)
);

-- Indices
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_provider_id) WHERE oauth_provider IS NOT NULL;
CREATE INDEX idx_characters_user_id ON characters(user_id);
CREATE INDEX idx_characters_lodestone_id ON characters(lodestone_id) WHERE lodestone_id IS NOT NULL;
CREATE INDEX idx_characters_data_center_server ON characters(data_center, server);
CREATE INDEX idx_characters_lfs ON characters(data_center, looking_for_static) WHERE looking_for_static = TRUE AND is_verified = TRUE;
CREATE INDEX idx_character_jobs_character_id ON character_jobs(character_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### 4.2 Indices

| Tabela           | Indice                              | Tipo             | Justificativa                 |
| ---------------- | ----------------------------------- | ---------------- | ----------------------------- |
| `users`          | `email`                             | UNIQUE           | Login por email               |
| `users`          | `oauth_provider, oauth_provider_id` | BTREE (partial)  | Login via OAuth               |
| `characters`     | `user_id`                           | BTREE            | Buscar personagens do usuario |
| `characters`     | `lodestone_id`                      | UNIQUE (partial) | Vinculo unico com Lodestone   |
| `characters`     | `data_center, server`               | BTREE            | Filtros de busca              |
| `characters`     | `data_center, looking_for_static`   | BTREE (partial)  | Jogadores LFS por DC          |
| `character_jobs` | `character_id`                      | BTREE            | Jobs do personagem            |
| `character_jobs` | `character_id, job_name`            | UNIQUE           | Evitar job duplicado          |
| `refresh_tokens` | `token_hash`                        | UNIQUE (partial) | Busca por token ativo         |
| `refresh_tokens` | `user_id`                           | BTREE            | Revogar tokens do usuario     |

---

## 5. Integracao Entre Modulos

### 5.1 Dependencias

```
auth-profile
    |
    +--> notification (envia emails de verificacao, reset, etc.)
    |
    +--> integration/lodestone (verificacao e importacao de dados)
    |
    <-- matching (consome CharacterVerifiedEvent, ProfileUpdatedEvent)
    |
    <-- static_ (consome dados de Character para memberships)
    |
    <-- reputation (consome dados de Character para reviews)
    |
    <-- chat (consome dados de User para mensagens)
    |
    <-- premium (atualiza isPremium e premiumExpiresAt no User)
```

### 5.2 Eventos Publicados e Consumidos

| Direcao | Evento                        | Consumidor                | Acao                             |
| ------- | ----------------------------- | ------------------------- | -------------------------------- |
| Saida   | `UserRegisteredEvent`         | `NotificationService`     | Envia email de verificacao       |
| Saida   | `PasswordResetRequestedEvent` | `NotificationService`     | Envia email de reset             |
| Saida   | `ProfileUpdatedEvent`         | `MatchingService`         | Recalcula matches do usuario     |
| Saida   | `CharacterVerifiedEvent`      | `MatchingService`         | Ativa matching para o personagem |
| Entrada | `SubscriptionActivatedEvent`  | `AuthService/UserService` | Atualiza isPremium=true          |
| Entrada | `SubscriptionExpiredEvent`    | `AuthService/UserService` | Atualiza isPremium=false         |

---

## 6. Tratamento de Erros

| HTTP | Tipo RFC 7807                      | Cenario                                                  |
| ---- | ---------------------------------- | -------------------------------------------------------- |
| 400  | `/errors/validation`               | Campos invalidos no request                              |
| 400  | `/errors/password-mismatch`        | Senha e confirmacao nao coincidem                        |
| 401  | `/errors/invalid-credentials`      | Email ou senha incorretos                                |
| 401  | `/errors/token-expired`            | Access token expirado                                    |
| 401  | `/errors/refresh-token-invalid`    | Refresh token invalido ou revogado                       |
| 403  | `/errors/account-locked`           | Conta bloqueada por tentativas excessivas                |
| 403  | `/errors/account-not-verified`     | Email nao verificado                                     |
| 404  | `/errors/user-not-found`           | Usuario nao encontrado                                   |
| 404  | `/errors/character-not-found`      | Personagem nao encontrado                                |
| 409  | `/errors/email-already-exists`     | Email ja registrado                                      |
| 409  | `/errors/lodestone-already-linked` | Lodestone ID ja vinculado a outra conta (RN04)           |
| 409  | `/errors/job-already-exists`       | Job ja existe no personagem                              |
| 422  | `/errors/verification-failed`      | Codigo de verificacao nao encontrado na bio do Lodestone |
| 422  | `/errors/verification-expired`     | Codigo de verificacao expirado                           |
| 429  | `/errors/rate-limit`               | Limite de tentativas de login excedido                   |
| 502  | `/errors/lodestone-unavailable`    | Lodestone/XIVAPI indisponivel                            |
