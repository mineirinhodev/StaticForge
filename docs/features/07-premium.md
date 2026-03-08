# Feature 07 - Premium e Assinaturas

## 1. Visao Geral

O modulo Premium gerencia o modelo de monetizacao freemium da plataforma. Inclui a integracao com o Stripe para processamento de pagamentos, gerenciamento de assinaturas (mensal/anual), trial gratuito, e a logica de enforcement dos limites free/premium que afetam todos os demais modulos do sistema.

### 1.1 Rastreabilidade

| Tipo | IDs |
|------|-----|
| **Regras de Negocio** | RN42, RN43, RN44, RN45 |
| **Casos de Uso** | UC26 (Assinar Premium), UC27 (Gerenciar Assinatura), UC28 (Cancelar Premium) |

### 1.2 Limites Free vs Premium

| Recurso | Free | Premium |
|---------|------|---------|
| Statics criadas | 1 | Ilimitadas |
| Sugestoes de match por semana | 5 | Ilimitadas |
| Conversas diretas ativas | 20 | Ilimitadas |
| Criar eventos PUG publicos | Nao | Sim |
| Exportar calendario (iCal) | Nao | Sim |
| Filtros de busca avancados | Nao | Sim |
| Badge premium no perfil | Nao | Sim |
| Perfil destacado na busca | Nao | Sim |
| Lembretes personalizados | Nao | Sim |
| Webhooks Discord | Nao | Sim |

---

## 2. Backend

### 2.1 Entities (JPA)

#### Subscription

```java
@Entity
@Table(name = "subscriptions")
public class Subscription extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan; // MONTHLY, ANNUAL

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status; // ACTIVE, CANCELED, PAST_DUE, EXPIRED, TRIALING

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    @Column(name = "trial_used", nullable = false)
    private boolean trialUsed = false;

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 100, unique = true)
    private String stripeSubscriptionId;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew = true;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL)
    @OrderBy("paidAt DESC")
    private List<PaymentHistory> payments = new ArrayList<>();
}
```

#### Enums

```java
public enum SubscriptionPlan {
    MONTHLY,
    ANNUAL
}

public enum SubscriptionStatus {
    ACTIVE,
    CANCELED,    // cancelado, mas ativo ate o fim do periodo
    PAST_DUE,    // pagamento falhou, em periodo de graca
    EXPIRED,     // expirado, sem acesso premium
    TRIALING     // periodo de trial (7 dias)
}
```

#### PaymentHistory

```java
@Entity
@Table(name = "payment_history")
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency; // USD, EUR, BRL, JPY

    @Column(name = "stripe_payment_id", length = 100, unique = true)
    private String stripePaymentId;

    @Column(name = "stripe_invoice_id", length = 100)
    private String stripeInvoiceId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // SUCCEEDED, FAILED, REFUNDED, PENDING

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

public enum PaymentStatus {
    SUCCEEDED,
    FAILED,
    REFUNDED,
    PENDING
}
```

### 2.2 DTOs (Java Records)

#### Request DTOs

```java
// Iniciar assinatura
public record CreateSubscriptionRequest(
    @NotNull
    SubscriptionPlan plan, // MONTHLY ou ANNUAL

    String successUrl, // URL de retorno apos pagamento bem-sucedido
    String cancelUrl   // URL de retorno se cancelar checkout
) {}

// Ativar trial
public record ActivateTrialRequest(
    // sem campos; trial e ativado para o usuario autenticado
) {}
```

#### Response DTOs

```java
// Plano disponivel
public record PlanResponse(
    String planId,
    SubscriptionPlan plan,
    String name,             // "Monthly Premium", "Annual Premium"
    BigDecimal price,
    String currency,
    String interval,         // "month", "year"
    BigDecimal savingsPercent, // 0 para mensal, ~16% para anual
    List<String> features    // lista de features incluidas
) {}

// Assinatura do usuario
public record SubscriptionResponse(
    UUID id,
    SubscriptionPlan plan,
    SubscriptionStatus status,
    LocalDate startDate,
    LocalDate endDate,
    boolean cancelAtPeriodEnd,
    boolean autoRenew,
    boolean trialUsed,
    Instant createdAt
) {}

// Checkout session (redirecionar para Stripe)
public record CheckoutSessionResponse(
    String sessionId,
    String checkoutUrl // URL do Stripe Checkout
) {}

// Historico de pagamento
public record PaymentHistoryResponse(
    UUID id,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    Instant paidAt,
    String failureReason
) {}
```

#### Mapper (MapStruct)

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriptionMapper {
    SubscriptionResponse toResponse(Subscription subscription);
    PaymentHistoryResponse toPaymentResponse(PaymentHistory payment);
    List<PaymentHistoryResponse> toPaymentResponseList(List<PaymentHistory> payments);
}
```

### 2.3 Repository

```java
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserId(UUID userId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate <= :date")
    List<Subscription> findExpiringSoon(@Param("date") LocalDate date);

    @Query("SELECT s FROM Subscription s WHERE s.status IN ('ACTIVE', 'TRIALING') AND s.endDate < :today")
    List<Subscription> findExpired(@Param("today") LocalDate today);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'CANCELED' AND s.endDate < :today")
    List<Subscription> findCanceledAndExpired(@Param("today") LocalDate today);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE' AND s.endDate < :gracePeriodEnd")
    List<Subscription> findPastDueExpired(@Param("gracePeriodEnd") LocalDate gracePeriodEnd);
}

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

    List<PaymentHistory> findBySubscriptionIdOrderByPaidAtDesc(UUID subscriptionId);

    Page<PaymentHistory> findBySubscriptionId(UUID subscriptionId, Pageable pageable);

    Optional<PaymentHistory> findByStripePaymentId(String stripePaymentId);
}
```

### 2.4 Service

#### SubscriptionService

```java
@Service
@Transactional
public class SubscriptionService {

    // Dependencias: SubscriptionRepository, PaymentHistoryRepository, UserRepository,
    //               StripeService, SubscriptionMapper, ApplicationEventPublisher

    /**
     * Listar planos disponiveis.
     * Retorna lista de PlanResponse com precos e features de cada plano.
     * Nao requer autenticacao.
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> getAvailablePlans();

    /**
     * Iniciar assinatura via Stripe Checkout.
     * Fluxo:
     * 1. Verifica se o usuario ja tem assinatura ativa (se sim, erro)
     * 2. Busca ou cria Stripe Customer para o usuario
     * 3. Cria Stripe Checkout Session com o priceId correspondente ao plano
     * 4. Retorna CheckoutSessionResponse com URL de checkout
     *
     * Nota: a Subscription so e criada apos confirmacao via webhook do Stripe
     *
     * Regras: RN42 (mensal ou anual)
     */
    public CheckoutSessionResponse createSubscription(UUID userId, CreateSubscriptionRequest request);

    /**
     * Obter assinatura do usuario autenticado.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getMySubscription(UUID userId);

    /**
     * Cancelar assinatura.
     * Fluxo:
     * 1. Busca assinatura do usuario
     * 2. Valida que status e ACTIVE ou TRIALING
     * 3. Atualiza cancelAtPeriodEnd=true no Stripe
     * 4. Atualiza status local para CANCELED
     * 5. Recursos premium continuam ativos ate endDate (RN43)
     * 6. Publica SubscriptionCanceledEvent
     *
     * Regras: RN43 (recursos ativos ate fim do periodo)
     */
    public SubscriptionResponse cancelSubscription(UUID userId);

    /**
     * Reativar assinatura cancelada (antes do fim do periodo).
     * Fluxo:
     * 1. Busca assinatura com status CANCELED
     * 2. Valida que endDate ainda nao passou
     * 3. Remove cancelAtPeriodEnd no Stripe
     * 4. Atualiza status para ACTIVE
     * 5. Publica SubscriptionReactivatedEvent
     */
    public SubscriptionResponse reactivateSubscription(UUID userId);

    /**
     * Ativar trial de 7 dias.
     * Fluxo:
     * 1. Verifica que o usuario nunca usou trial (trialUsed flag) -- RN45
     * 2. Verifica que nao tem assinatura ativa
     * 3. Cria Subscription com status=TRIALING, endDate=hoje+7dias
     * 4. Atualiza User.isPremium=true
     * 5. Publica SubscriptionActivatedEvent
     *
     * Regras: RN45 (trial unico por conta)
     */
    public SubscriptionResponse activateTrial(UUID userId);

    /**
     * Obter historico de pagamentos.
     */
    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponse> getPaymentHistory(UUID userId, Pageable pageable);

    /**
     * Processar expiracao de assinatura (chamado pelo SubscriptionScheduler e webhooks).
     * Fluxo:
     * 1. Atualiza status para EXPIRED
     * 2. Atualiza User.isPremium=false, User.premiumExpiresAt=null
     * 3. Publica SubscriptionExpiredEvent
     *
     * Regras: RN44 (downgrade: statics extras em read-only, limits free)
     */
    public void expireSubscription(UUID subscriptionId);
}
```

#### StripeService

```java
@Service
public class StripeService {

    // Dependencias: Stripe API (stripe-java SDK), configuracao de chaves

    /**
     * Criar ou recuperar Stripe Customer.
     * Usa email do usuario como identificador.
     * Armazena stripeCustomerId na Subscription.
     */
    public String getOrCreateCustomer(String email, UUID userId);

    /**
     * Criar Checkout Session.
     * Configura:
     * - mode: subscription
     * - line_items: priceId do plano (monthly/annual)
     * - success_url e cancel_url
     * - metadata: userId
     * - subscription_data.trial_period_days (se trial)
     */
    public Session createCheckoutSession(String customerId, String priceId,
                                          String successUrl, String cancelUrl,
                                          Map<String, String> metadata);

    /**
     * Cancelar assinatura no Stripe (cancel_at_period_end).
     */
    public void cancelSubscription(String stripeSubscriptionId);

    /**
     * Reativar assinatura no Stripe (remover cancel_at_period_end).
     */
    public void reactivateSubscription(String stripeSubscriptionId);

    /**
     * Construir e verificar evento do webhook.
     * Valida assinatura HMAC do Stripe.
     */
    public Event constructWebhookEvent(String payload, String sigHeader);
}
```

#### StripeWebhookController

```java
@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {

    /**
     * Recebe webhooks do Stripe.
     * Endpoint publico (sem JWT), mas verificado por assinatura HMAC.
     *
     * Eventos tratados:
     *
     * checkout.session.completed:
     *   1. Extrai userId dos metadata
     *   2. Cria Subscription no DB com dados do Stripe
     *   3. Atualiza User.isPremium=true, User.premiumExpiresAt
     *   4. Registra PaymentHistory
     *   5. Publica SubscriptionActivatedEvent
     *
     * invoice.paid:
     *   1. Busca Subscription por stripeSubscriptionId
     *   2. Atualiza endDate para novo periodo
     *   3. Registra PaymentHistory com status SUCCEEDED
     *   4. Se estava PAST_DUE, restaura para ACTIVE
     *
     * invoice.payment_failed:
     *   1. Busca Subscription
     *   2. Atualiza status para PAST_DUE
     *   3. Registra PaymentHistory com status FAILED e failureReason
     *   4. Publica PaymentFailedEvent (notificacao ao usuario)
     *
     * customer.subscription.deleted:
     *   1. Busca Subscription
     *   2. Chama expireSubscription()
     *   3. Publica SubscriptionExpiredEvent
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader);
}
```

#### Annotation @RequiresPremium

```java
// Annotation
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPremium {
    String message() default "This feature requires a premium subscription";
}

// Aspect
@Aspect
@Component
public class PremiumAspect {

    @Around("@annotation(requiresPremium)")
    public Object checkPremium(ProceedingJoinPoint joinPoint, RequiresPremium requiresPremium) {
        // 1. Extrai userId do SecurityContext
        // 2. Busca User por ID
        // 3. Se !isPremium, lanca PremiumRequiredException (HTTP 403)
        // 4. Se isPremium, prossegue com joinPoint.proceed()
    }
}
```

#### Eventos Publicados

| Evento | Publicado por | Dados | Consumido por |
|--------|--------------|-------|---------------|
| `SubscriptionActivatedEvent` | `SubscriptionService` / Webhook | userId, plan, endDate | `UserService` (atualiza isPremium), `NotificationService` |
| `SubscriptionCanceledEvent` | `SubscriptionService.cancel` | userId, endDate | `NotificationService` (confirma cancelamento) |
| `SubscriptionReactivatedEvent` | `SubscriptionService.reactivate` | userId | `NotificationService` |
| `SubscriptionExpiredEvent` | `SubscriptionService.expire` | userId | `UserService` (remove isPremium), `StaticService` (read-only statics extras), `NotificationService` |
| `PaymentFailedEvent` | Webhook `invoice.payment_failed` | userId, failureReason | `NotificationService` (alerta usuario) |

### 2.5 Controller

#### SubscriptionController

```
@RestController
@RequestMapping("/api/v1/subscriptions")
```

| Metodo | Path | Descricao | Auth | Request Body | Response Body | Status |
|--------|------|-----------|------|-------------|---------------|--------|
| GET | `/plans` | Listar planos | Nao | - | `List<PlanResponse>` | 200 |
| POST | `/` | Iniciar assinatura | Sim | `CreateSubscriptionRequest` | `CheckoutSessionResponse` | 201 |
| GET | `/me` | Minha assinatura | Sim | - | `SubscriptionResponse` | 200 |
| PUT | `/me/cancel` | Cancelar assinatura | Sim | - | `SubscriptionResponse` | 200 |
| PUT | `/me/reactivate` | Reativar assinatura | Sim | - | `SubscriptionResponse` | 200 |
| POST | `/trial` | Ativar trial | Sim | - | `SubscriptionResponse` | 201 |
| GET | `/me/payments` | Historico de pagamentos | Sim | - | `Page<PaymentHistoryResponse>` | 200 |

#### StripeWebhookController

| Metodo | Path | Descricao | Auth |
|--------|------|-----------|------|
| POST | `/api/v1/webhooks/stripe` | Webhook Stripe | Nao (verificado por assinatura HMAC) |

**Exemplos de Request/Response:**

```json
// GET /api/v1/subscriptions/plans
// Response (200):
[
  {
    "planId": "price_monthly_001",
    "plan": "MONTHLY",
    "name": "Monthly Premium",
    "price": 4.99,
    "currency": "USD",
    "interval": "month",
    "savingsPercent": 0,
    "features": [
      "Unlimited statics",
      "Unlimited match suggestions",
      "Unlimited direct messages",
      "Create PUG events",
      "Export calendar (iCal)",
      "Advanced search filters",
      "Premium badge",
      "Priority support"
    ]
  },
  {
    "planId": "price_annual_001",
    "plan": "ANNUAL",
    "name": "Annual Premium",
    "price": 49.99,
    "currency": "USD",
    "interval": "year",
    "savingsPercent": 16.5,
    "features": [
      "All Monthly features",
      "Save 16.5% vs monthly"
    ]
  }
]
```

```json
// POST /api/v1/subscriptions
// Request:
{
  "plan": "MONTHLY",
  "successUrl": "https://ffxiv-planner.com/account/subscription?success=true",
  "cancelUrl": "https://ffxiv-planner.com/premium?canceled=true"
}

// Response (201):
{
  "sessionId": "cs_test_abc123...",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_abc123..."
}
```

```json
// GET /api/v1/subscriptions/me
// Response (200):
{
  "id": "770e8400-e29b-41d4-a716-446655440010",
  "plan": "MONTHLY",
  "status": "ACTIVE",
  "startDate": "2026-02-01",
  "endDate": "2026-03-01",
  "cancelAtPeriodEnd": false,
  "autoRenew": true,
  "trialUsed": true,
  "createdAt": "2026-02-01T00:00:00Z"
}
```

### 2.6 Seguranca e Autorizacao

```java
// SecurityConfig.java
.requestMatchers("/api/v1/subscriptions/plans").permitAll()
.requestMatchers("/api/v1/webhooks/stripe").permitAll()
.requestMatchers("/api/v1/subscriptions/**").authenticated()
```

- Webhook do Stripe e publico, mas verificado pela assinatura HMAC usando o signing secret
- Todos os endpoints de assinatura requerem autenticacao
- Verificacao de ownership: cada usuario so pode acessar/modificar sua propria assinatura

#### Rate Limiting

```
- POST /api/v1/subscriptions: 5 requests/hora por usuario (evitar abuse)
- POST /api/v1/subscriptions/trial: 1 request/dia por usuario
```

### 2.7 Validacoes

| Campo | Regra | DTO |
|-------|-------|-----|
| `plan` | Obrigatorio, enum MONTHLY/ANNUAL | CreateSubscriptionRequest |
| `successUrl` | URL valida | CreateSubscriptionRequest |
| `cancelUrl` | URL valida | CreateSubscriptionRequest |
| Assinatura ativa | Usuario nao pode ter assinatura ja ativa | Service (createSubscription) |
| Trial unico | trialUsed deve ser false | Service (activateTrial) - RN45 |
| Cancelamento | Status deve ser ACTIVE ou TRIALING | Service (cancelSubscription) |
| Reativacao | Status deve ser CANCELED e endDate > hoje | Service (reactivateSubscription) |

### 2.8 Cache (Redis)

| Chave | Tipo | TTL | Descricao | Invalidacao |
|-------|------|-----|-----------|-------------|
| `subscription:{userId}` | String (JSON) | 5 min | Cache da assinatura do usuario | Qualquer mudanca de status |
| `plans:available` | String (JSON) | 1 hora | Cache dos planos disponiveis | Mudanca de precos (manual) |
| `premium:check:{userId}` | Boolean | 1 min | Cache rapido de verificacao isPremium | Mudanca de status |

### 2.9 Jobs Agendados

| Job | Frequencia | Descricao |
|-----|-----------|-----------|
| `SubscriptionScheduler` | Diario (01:00 UTC) | 1. Busca assinaturas com endDate < hoje e status ACTIVE/TRIALING -> expira (RN44). 2. Busca assinaturas PAST_DUE com mais de 7 dias -> expira. 3. Busca assinaturas expirando em 3 dias -> envia alerta (NotificationService). 4. Busca assinaturas CANCELED com endDate < hoje -> atualiza para EXPIRED e remove isPremium do User. |

---

## 3. Frontend

### 3.1 Paginas e Rotas

| Rota | Renderizacao | Descricao |
|------|-------------|-----------|
| `/[locale]/(main)/premium` | CSR | Pagina de planos premium com comparacao free vs premium |
| `/[locale]/(main)/account/subscription` | CSR | Gerenciar assinatura atual |
| `/[locale]/(main)/account/settings` | CSR | Configuracoes da conta (inclui secao de assinatura) |

### 3.2 Componentes React

```
premium/page.tsx                   -- Client Component
  PremiumHero                      -- Banner com proposta de valor
  PlanComparison                   -- Tabela comparativa free vs premium
    PlanCard                       -- Card de cada plano com preco e features
      PriceDisplay                 -- Preco com periodo (mes/ano)
      FeatureList                  -- Lista de features inclusas
      SubscribeButton              -- Botao de assinar (redireciona para Stripe)
  FrequentlyAskedQuestions         -- FAQ sobre premium
  TestimonialCarousel              -- Depoimentos de usuarios premium

account/subscription/page.tsx      -- Client Component
  SubscriptionStatus               -- Status atual da assinatura
    PlanBadge                      -- Badge do plano (MONTHLY/ANNUAL)
    StatusIndicator                -- Indicador visual do status
    ExpirationDate                 -- Data de expiracao
    RenewalInfo                    -- Info de renovacao automatica
  SubscriptionActions              -- Acoes disponiveis
    CancelButton                   -- Cancelar assinatura (com confirmacao)
    ReactivateButton               -- Reativar (se cancelado)
    ChangePlanButton               -- Trocar plano
  PaymentHistoryTable              -- Tabela de historico de pagamentos
    PaymentRow                     -- Linha de cada pagamento
      StatusBadge                  -- Badge de status (SUCCEEDED, FAILED, etc.)
      AmountDisplay                -- Valor formatado com moeda
      DateDisplay                  -- Data do pagamento
  TrialBanner                      -- Banner de trial (se nunca usou)
    ActivateTrialButton            -- Botao para ativar trial
```

### 3.3 Estado e Data Fetching

#### Hooks TanStack Query

```typescript
// hooks/use-subscription.ts

function usePlans() {
  return useQuery({
    queryKey: ['subscription', 'plans'],
    queryFn: () => api.get('/subscriptions/plans'),
    staleTime: 60 * 60 * 1000, // 1 hora
  });
}

function useMySubscription() {
  return useQuery({
    queryKey: ['subscription', 'me'],
    queryFn: () => api.get('/subscriptions/me'),
    enabled: authStore.isAuthenticated,
    retry: false, // 404 e esperado se nao tem assinatura
  });
}

function useCreateSubscription() {
  return useMutation({
    mutationFn: (data: CreateSubscriptionRequest) =>
      api.post('/subscriptions', data),
    onSuccess: (response) => {
      // Redireciona para Stripe Checkout
      window.location.href = response.checkoutUrl;
    },
  });
}

function useCancelSubscription() {
  return useMutation({
    mutationFn: () => api.put('/subscriptions/me/cancel'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscription', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['profile', 'me'] });
    },
  });
}

function useReactivateSubscription() {
  return useMutation({
    mutationFn: () => api.put('/subscriptions/me/reactivate'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscription', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['profile', 'me'] });
    },
  });
}

function useActivateTrial() {
  return useMutation({
    mutationFn: () => api.post('/subscriptions/trial'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscription', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['profile', 'me'] });
    },
  });
}

function usePaymentHistory(page: number = 0) {
  return useQuery({
    queryKey: ['subscription', 'payments', page],
    queryFn: () => api.get(`/subscriptions/me/payments?page=${page}&size=10`),
    enabled: authStore.isAuthenticated,
  });
}
```

### 3.4 Formularios e Validacao (Zod)

```typescript
// schemas/subscription.ts
import { z } from 'zod';

export const createSubscriptionSchema = z.object({
  plan: z.enum(['MONTHLY', 'ANNUAL']),
  successUrl: z.string().url().optional(),
  cancelUrl: z.string().url().optional(),
});

// Nao ha formularios complexos -- a maior parte do fluxo de pagamento
// e gerenciada pelo Stripe Checkout (hosted page).
```

### 3.5 WebSocket

Nao aplicavel diretamente a este modulo. Notificacoes sobre status de assinatura sao enviadas via canal de notificacoes pessoais (`/user/queue/notifications`).

### 3.6 Internacionalizacao

**Namespaces i18n:**

| Namespace | Uso |
|-----------|-----|
| `premium.*` | Pagina de planos e comparacao |
| `premium.plans.*` | Nomes e descricoes dos planos |
| `premium.features.*` | Lista de features premium |
| `subscription.*` | Gerenciamento de assinatura |
| `subscription.status.*` | Labels de status (ACTIVE, CANCELED, etc.) |
| `subscription.actions.*` | Botoes e confirmacoes |
| `payment.*` | Historico de pagamentos |
| `trial.*` | Banner e instrucoes do trial |

**Exemplo de chaves:**

```json
{
  "premium": {
    "title": "Upgrade to Premium",
    "subtitle": "Unlock the full potential of FFXIV Static Planner",
    "plans": {
      "monthly": {
        "name": "Monthly",
        "price": "$4.99/month"
      },
      "annual": {
        "name": "Annual",
        "price": "$49.99/year",
        "savings": "Save 16.5%"
      }
    },
    "features": {
      "unlimitedStatics": "Create unlimited statics",
      "unlimitedMatches": "Unlimited match suggestions",
      "unlimitedChats": "Unlimited direct messages",
      "pugEvents": "Create public PUG events",
      "icalExport": "Export calendar (iCal)"
    }
  },
  "subscription": {
    "status": {
      "active": "Active",
      "canceled": "Canceled (active until {date})",
      "expired": "Expired",
      "trialing": "Trial (ends {date})",
      "pastDue": "Payment past due"
    },
    "actions": {
      "cancel": "Cancel subscription",
      "cancelConfirm": "Are you sure? You'll keep premium access until {date}.",
      "reactivate": "Reactivate subscription"
    }
  }
}
```

---

## 4. Banco de Dados

### 4.1 DDL

```sql
-- Migration: V8__create_subscriptions_payments.sql

-- Tabela de assinaturas
CREATE TABLE subscriptions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    plan                    VARCHAR(20) NOT NULL, -- MONTHLY, ANNUAL
    status                  VARCHAR(20) NOT NULL, -- ACTIVE, CANCELED, PAST_DUE, EXPIRED, TRIALING
    start_date              DATE NOT NULL,
    end_date                DATE NOT NULL,
    cancel_at_period_end    BOOLEAN NOT NULL DEFAULT FALSE,
    trial_used              BOOLEAN NOT NULL DEFAULT FALSE,
    stripe_customer_id      VARCHAR(100),
    stripe_subscription_id  VARCHAR(100) UNIQUE,
    auto_renew              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabela de historico de pagamentos
CREATE TABLE payment_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id     UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    amount              DECIMAL(10, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    stripe_payment_id   VARCHAR(100) UNIQUE,
    stripe_invoice_id   VARCHAR(100),
    status              VARCHAR(20) NOT NULL, -- SUCCEEDED, FAILED, REFUNDED, PENDING
    paid_at             TIMESTAMPTZ,
    failure_reason      VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indices
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_end_date ON subscriptions(end_date);
CREATE INDEX idx_subscriptions_stripe_sub_id ON subscriptions(stripe_subscription_id) WHERE stripe_subscription_id IS NOT NULL;
CREATE INDEX idx_subscriptions_stripe_cust_id ON subscriptions(stripe_customer_id) WHERE stripe_customer_id IS NOT NULL;
CREATE INDEX idx_payment_history_subscription_id ON payment_history(subscription_id);
CREATE INDEX idx_payment_history_stripe_payment_id ON payment_history(stripe_payment_id) WHERE stripe_payment_id IS NOT NULL;
CREATE INDEX idx_payment_history_paid_at ON payment_history(paid_at DESC);
```

### 4.2 Indices

| Tabela | Indice | Tipo | Justificativa |
|--------|--------|------|---------------|
| `subscriptions` | `user_id` | UNIQUE | Uma assinatura por usuario |
| `subscriptions` | `status` | BTREE | Filtro por status no scheduler |
| `subscriptions` | `end_date` | BTREE | Busca de assinaturas expirando |
| `subscriptions` | `stripe_subscription_id` | UNIQUE (partial) | Busca por ID do Stripe |
| `subscriptions` | `stripe_customer_id` | BTREE (partial) | Busca por customer do Stripe |
| `payment_history` | `subscription_id` | BTREE | Historico por assinatura |
| `payment_history` | `stripe_payment_id` | UNIQUE (partial) | Idempotencia de webhooks |
| `payment_history` | `paid_at` | BTREE DESC | Ordenacao cronologica |

---

## 5. Integracao Entre Modulos

### 5.1 Dependencias

```
premium
    |
    +--> auth-profile (atualiza isPremium no User)
    |
    +--> integration/stripe (pagamentos e webhooks)
    |
    +--> notification (alertas de pagamento, expiracao, etc.)
    |
    <-- static_ (verifica isPremium para limite de criacao - RN10)
    |
    <-- matching (verifica isPremium para limite de sugestoes - RN20)
    |
    <-- chat (verifica isPremium para limite de DMs - RN39)
    |
    <-- calendar (verifica isPremium para criacao de PUGs e iCal - RN27, RN28)
    |
    <-- reputation (verifica isPremium para selo de verificacao)
```

### 5.2 Enforcement de Limites (Como os Modulos Verificam Premium)

```java
// Verificacao direta no Service (exemplo: StaticService)
public Static createStatic(UUID userId, CreateStaticRequest request) {
    User user = userRepository.findById(userId).orElseThrow();
    if (!user.isPremium()) {
        long count = staticRepository.countByLeaderUserId(userId);
        if (count >= 1) {
            throw new PremiumRequiredException("Free users can create up to 1 static");
        }
    }
    // ... criar static
}

// Verificacao via Annotation (exemplo: CalendarService)
@RequiresPremium(message = "Creating PUG events requires premium")
public RaidEvent createPugEvent(UUID userId, CreateEventRequest request) {
    // ... criar evento PUG
}

// Verificacao via contador Redis (exemplo: MatchingService)
public List<MatchSuggestion> getMatches(UUID userId) {
    User user = userRepository.findById(userId).orElseThrow();
    if (!user.isPremium()) {
        String key = "count:matches:" + userId + ":" + getCurrentWeek();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count > 5) {
            throw new PremiumRequiredException("Free users receive up to 5 match suggestions per week");
        }
    }
    // ... retornar matches
}
```

### 5.3 Eventos Publicados e Consumidos

| Direcao | Evento | Consumidor | Acao |
|---------|--------|-----------|------|
| Saida | `SubscriptionActivatedEvent` | `UserService` | isPremium=true, premiumExpiresAt=endDate |
| Saida | `SubscriptionExpiredEvent` | `UserService` | isPremium=false, premiumExpiresAt=null |
| Saida | `SubscriptionExpiredEvent` | `StaticService` | Statics extras em read-only (RN44) |
| Saida | `SubscriptionCanceledEvent` | `NotificationService` | Notifica usuario da data final |
| Saida | `PaymentFailedEvent` | `NotificationService` | Alerta usuario sobre falha |

---

## 6. Tratamento de Erros

| HTTP | Tipo RFC 7807 | Cenario |
|------|---------------|---------|
| 400 | `/errors/validation` | Campos invalidos no request |
| 400 | `/errors/invalid-plan` | Plano nao reconhecido |
| 402 | `/errors/payment-required` | Pagamento falhou ou e necessario |
| 403 | `/errors/premium-required` | Funcionalidade requer premium (via @RequiresPremium) |
| 404 | `/errors/subscription-not-found` | Usuario nao tem assinatura |
| 409 | `/errors/subscription-already-active` | Ja tem assinatura ativa |
| 409 | `/errors/trial-already-used` | Trial ja foi utilizado (RN45) |
| 409 | `/errors/cannot-reactivate` | Assinatura ja expirou, nao pode reativar |
| 422 | `/errors/cannot-cancel` | Assinatura nao esta em status cancelavel |
| 500 | `/errors/stripe-error` | Erro na comunicacao com Stripe |
| 502 | `/errors/stripe-unavailable` | Stripe indisponivel |
