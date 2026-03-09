# Feature 06 - Chat

## 1. Visao Geral

O modulo de Chat implementa a comunicacao em tempo real entre usuarios da plataforma via WebSocket (STOMP sobre SockJS). Suporta dois tipos de chat: mensagens diretas (DM) entre dois usuarios e chat de grupo automatico vinculado a cada static. O sistema inclui indicadores de digitacao, confirmacao de leitura, presenca online, e escalabilidade multi-instancia via Redis Pub/Sub.

### 1.1 Rastreabilidade

| Tipo                  | IDs                                                                            |
| --------------------- | ------------------------------------------------------------------------------ |
| **Regras de Negocio** | RN37, RN38, RN39, RN40, RN41                                                   |
| **Casos de Uso**      | UC23 (Enviar Mensagem Direta), UC24 (Chat de Static), UC25 (Reportar Mensagem) |

---

## 2. Backend

### 2.1 Entities (JPA)

#### ChatRoom

```java
@Entity
@Table(name = "chat_rooms")
public class ChatRoom extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ChatRoomType type; // DM, STATIC

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "static_id")
    private Static static_; // null para DMs

    @Column(length = 100)
    private String name; // nome do chat (para groups; null para DMs)

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoomMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    @OrderBy("sentAt DESC")
    private List<Message> messages = new ArrayList<>();
}

public enum ChatRoomType {
    DM,     // mensagem direta (2 membros)
    STATIC  // chat de grupo da static (N membros)
}
```

#### ChatRoomMember

```java
@Entity
@Table(name = "chat_room_members")
public class ChatRoomMember extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt; // null = membro ativo

    @Column(name = "last_read_at")
    private Instant lastReadAt; // timestamp da ultima mensagem lida

    @Column(name = "is_muted", nullable = false)
    private boolean isMuted = false;
}
```

#### Message

```java
@Entity
@Table(name = "messages")
public class Message extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt; // soft delete

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<MessageRead> reads = new ArrayList<>();
}
```

#### MessageRead

```java
@Entity
@Table(name = "message_reads")
public class MessageRead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;
}
```

### 2.2 DTOs (Java Records)

#### Request DTOs

```java
// Iniciar DM
public record CreateDmRequest(
    @NotNull
    UUID targetUserId
) {}

// Enviar mensagem (REST fallback)
public record SendMessageRequest(
    @NotBlank @Size(max = 4000)
    String content
) {}

// Mensagem via WebSocket (STOMP)
public record ChatMessageDto(
    UUID chatRoomId,
    String content
) {}

// Indicador de digitacao
public record TypingIndicatorDto(
    UUID chatRoomId,
    boolean isTyping
) {}

// Marcar como lido
public record ReadReceiptDto(
    UUID chatRoomId,
    UUID lastReadMessageId
) {}
```

#### Response DTOs

```java
// Chat Room
public record ChatRoomResponse(
    UUID id,
    ChatRoomType type,
    String name,           // nome da static ou nome do outro usuario (para DMs)
    UUID staticId,
    List<ChatMemberResponse> members,
    MessageResponse lastMessage,
    int unreadCount,
    boolean isActive,
    Instant createdAt
) {}

// Chat Room resumido (para listagem)
public record ChatRoomSummaryResponse(
    UUID id,
    ChatRoomType type,
    String name,
    String avatarUrl,       // avatar do outro usuario (DM) ou avatar da static
    MessageSummaryResponse lastMessage,
    int unreadCount,
    boolean isActive
) {}

// Membro do chat
public record ChatMemberResponse(
    UUID userId,
    String displayName,
    String avatarUrl,
    boolean isOnline,
    Instant lastSeen
) {}

// Mensagem
public record MessageResponse(
    UUID id,
    UUID chatRoomId,
    UUID senderId,
    String senderName,
    String senderAvatar,
    String content,
    Instant sentAt,
    Instant editedAt,
    boolean isDeleted,
    List<ReadReceiptResponse> reads
) {}

// Mensagem resumida
public record MessageSummaryResponse(
    String content,          // truncado a 100 chars
    String senderName,
    Instant sentAt
) {}

// Confirmacao de leitura
public record ReadReceiptResponse(
    UUID userId,
    Instant readAt
) {}

// Presenca online
public record PresenceResponse(
    UUID userId,
    boolean isOnline,
    Instant lastSeen
) {}
```

#### Mapper (MapStruct)

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMapper {
    ChatRoomResponse toResponse(ChatRoom room);
    ChatRoomSummaryResponse toSummary(ChatRoom room);
    MessageResponse toMessageResponse(Message message);
}
```

### 2.3 Repository

```java
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Query("SELECT r FROM ChatRoom r JOIN r.members m WHERE m.user.id = :userId AND m.leftAt IS NULL " +
           "AND r.isActive = true ORDER BY r.updatedAt DESC")
    List<ChatRoom> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT r FROM ChatRoom r WHERE r.type = 'DM' AND r.isActive = true " +
           "AND EXISTS (SELECT m1 FROM ChatRoomMember m1 WHERE m1.chatRoom = r AND m1.user.id = :user1Id AND m1.leftAt IS NULL) " +
           "AND EXISTS (SELECT m2 FROM ChatRoomMember m2 WHERE m2.chatRoom = r AND m2.user.id = :user2Id AND m2.leftAt IS NULL)")
    Optional<ChatRoom> findDmBetween(@Param("user1Id") UUID user1Id, @Param("user2Id") UUID user2Id);

    @Query("SELECT r FROM ChatRoom r WHERE r.type = 'STATIC' AND r.static_.id = :staticId AND r.isActive = true")
    Optional<ChatRoom> findByStaticId(@Param("staticId") UUID staticId);

    @Query("SELECT COUNT(r) FROM ChatRoom r JOIN r.members m WHERE m.user.id = :userId " +
           "AND m.leftAt IS NULL AND r.type = 'DM' AND r.isActive = true")
    int countActiveDmsByUserId(@Param("userId") UUID userId);
}

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);

    @Query("SELECT m FROM ChatRoomMember m WHERE m.chatRoom.id = :roomId AND m.leftAt IS NULL")
    List<ChatRoomMember> findActiveByRoomId(@Param("roomId") UUID roomId);
}

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :roomId AND m.deletedAt IS NULL " +
           "ORDER BY m.sentAt DESC")
    Page<Message> findByChatRoomId(@Param("roomId") UUID roomId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :roomId AND m.sentAt < :before " +
           "AND m.deletedAt IS NULL ORDER BY m.sentAt DESC")
    Page<Message> findByChatRoomIdBefore(@Param("roomId") UUID roomId,
                                         @Param("before") Instant before,
                                         Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :roomId AND m.deletedAt IS NULL " +
           "AND m.sentAt > :since AND m.sender.id != :userId")
    int countUnread(@Param("roomId") UUID roomId, @Param("since") Instant since,
                    @Param("userId") UUID userId);
}

public interface MessageReadRepository extends JpaRepository<MessageRead, UUID> {

    List<MessageRead> findByMessageId(UUID messageId);

    @Modifying
    @Query("INSERT INTO message_reads (id, message_id, user_id, read_at) " +
           "SELECT gen_random_uuid(), m.id, :userId, NOW() FROM messages m " +
           "WHERE m.chat_room_id = :roomId AND m.sent_at <= :upTo " +
           "AND NOT EXISTS (SELECT 1 FROM message_reads r WHERE r.message_id = m.id AND r.user_id = :userId)")
    void markAllAsRead(@Param("roomId") UUID roomId, @Param("userId") UUID userId,
                       @Param("upTo") Instant upTo);
}
```

### 2.4 Service

#### ChatService

```java
@Service
@Transactional
public class ChatService {

    /**
     * Listar conversas do usuario.
     * Retorna lista de ChatRoomSummaryResponse ordenada por ultima mensagem.
     * Inclui contagem de mensagens nao lidas.
     */
    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getMyChatRooms(UUID userId);

    /**
     * Iniciar conversa direta (DM).
     * Fluxo:
     * 1. Verifica que o target nao bloqueou o usuario (RN41)
     * 2. Verifica se ja existe DM entre os dois -> retorna existente
     * 3. Verifica limite free: max 20 DMs ativas (RN39)
     * 4. Cria ChatRoom type=DM com 2 membros
     * 5. Retorna ChatRoomResponse
     *
     * Regras: RN37 (DM entre qualquer par), RN39 (limite free), RN41 (bloqueio)
     */
    public ChatRoomResponse createDm(UUID userId, CreateDmRequest request);

    /**
     * Criar chat de grupo para static (interno - chamado via evento).
     * Fluxo:
     * 1. Cria ChatRoom type=STATIC vinculado a static
     * 2. Adiciona todos os membros ativos da static como membros do chat
     *
     * Regras: RN38 (chat automatico por static)
     */
    public ChatRoom createStaticChatRoom(UUID staticId);

    /**
     * Adicionar membro ao chat da static (interno - via evento MemberJoinedEvent).
     */
    public void addMemberToStaticChat(UUID staticId, UUID userId);

    /**
     * Remover membro do chat da static (interno - via evento MemberLeftEvent).
     */
    public void removeMemberFromStaticChat(UUID staticId, UUID userId);

    /**
     * Desativar chat da static (interno - via evento StaticDissolvedEvent).
     */
    public void deactivateStaticChat(UUID staticId);
}
```

#### MessageService

```java
@Service
@Transactional
public class MessageService {

    /**
     * Enviar mensagem (via REST fallback).
     * Fluxo:
     * 1. Verifica que o usuario e membro do chat room
     * 2. Verifica que o chat room esta ativo
     * 3. Sanitiza conteudo (XSS protection)
     * 4. Cria Message
     * 5. Publica no Redis channel ws:chat:{roomId} (para multi-instance)
     * 6. Broadcast via STOMP para /topic/chat.{roomId}
     *
     * Regras: RN40 (mensagens podem ser reportadas)
     */
    public MessageResponse sendMessage(UUID userId, UUID chatRoomId, SendMessageRequest request);

    /**
     * Enviar mensagem via WebSocket (chamado pelo STOMP controller).
     * Mesmo fluxo do REST, mas recebido via @MessageMapping.
     */
    public MessageResponse sendMessageWs(UUID userId, ChatMessageDto dto);

    /**
     * Obter historico de mensagens.
     * Retorna pagina de mensagens ordenadas por sentAt DESC.
     * Suporta cursor-based pagination (antes de um timestamp).
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(UUID userId, UUID chatRoomId, Pageable pageable);

    /**
     * Marcar mensagens como lidas.
     * Marca todas as mensagens do chat room ate o timestamp fornecido.
     * Broadcast read receipt via STOMP.
     */
    public void markAsRead(UUID userId, ReadReceiptDto dto);

    /**
     * Broadcast indicador de digitacao.
     * Nao persiste no banco, apenas broadcast via STOMP.
     */
    public void broadcastTyping(UUID userId, TypingIndicatorDto dto);
}
```

#### PresenceService

```java
@Service
public class PresenceService {

    /**
     * Atualizar presenca online do usuario no Redis.
     * Chamado periodicamente pelo heartbeat do WebSocket.
     * Chave: ws:presence:{userId}, TTL: 5 minutos.
     */
    public void updatePresence(UUID userId);

    /**
     * Verificar se usuario esta online.
     */
    public boolean isOnline(UUID userId);

    /**
     * Obter lista de usuarios online em um chat room.
     */
    public List<PresenceResponse> getRoomPresence(UUID chatRoomId);

    /**
     * Remover presenca (chamado no disconnect do WebSocket).
     */
    public void removePresence(UUID userId);
}
```

#### WebSocket Controller (STOMP)

```java
@Controller
public class ChatWebSocketController {

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto message, Principal principal) {
        UUID userId = extractUserId(principal);
        MessageResponse response = messageService.sendMessageWs(userId, message);
        // Broadcast para /topic/chat.{roomId}
        messagingTemplate.convertAndSend("/topic/chat." + message.chatRoomId(), response);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingIndicatorDto dto, Principal principal) {
        UUID userId = extractUserId(principal);
        messageService.broadcastTyping(userId, dto);
        // Broadcast para /topic/chat.{roomId} (exceto o remetente)
        messagingTemplate.convertAndSend("/topic/chat." + dto.chatRoomId(),
            new TypingEvent(userId, dto.isTyping()));
    }

    @MessageMapping("/chat.read")
    public void markRead(@Payload ReadReceiptDto dto, Principal principal) {
        UUID userId = extractUserId(principal);
        messageService.markAsRead(userId, dto);
        // Broadcast read receipt para /topic/chat.{roomId}
        messagingTemplate.convertAndSend("/topic/chat." + dto.chatRoomId(),
            new ReadEvent(userId, dto.lastReadMessageId()));
    }
}
```

#### Redis Pub/Sub para Multi-Instance

```java
@Configuration
public class ChatRedisConfig {

    /**
     * Configura Redis Pub/Sub para distribuir mensagens entre instancias.
     *
     * Fluxo:
     * 1. Instance A recebe mensagem via WebSocket
     * 2. Instance A salva no PostgreSQL
     * 3. Instance A publica no Redis channel ws:chat:{roomId}
     * 4. Todas as instances (incluindo A) recebem via subscriber
     * 5. Cada instance distribui para seus WebSocket clients locais
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        // Configura listener para canais ws:chat:*
    }
}

@Service
public class ChatRedisPublisher {
    public void publishMessage(UUID chatRoomId, MessageResponse message) {
        redisTemplate.convertAndSend("ws:chat:" + chatRoomId, message);
    }
}

@Service
public class ChatRedisSubscriber implements MessageListener {
    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        // Deserializa e distribui via STOMP para clients locais
    }
}
```

#### Eventos Consumidos

| Evento                 | Origem          | Acao                                     |
| ---------------------- | --------------- | ---------------------------------------- |
| `StaticCreatedEvent`   | `StaticService` | Cria chat room para a static (RN38)      |
| `MemberJoinedEvent`    | `StaticService` | Adiciona membro ao chat da static        |
| `MemberLeftEvent`      | `StaticService` | Remove membro do chat da static          |
| `StaticDissolvedEvent` | `StaticService` | Desativa chat da static                  |
| `UserBlockedEvent`     | `UserService`   | Desativa DM entre bloqueador e bloqueado |

### 2.5 Controller

#### ChatController (REST)

```
@RestController
@RequestMapping("/api/v1/chats")
```

| Metodo | Path             | Descricao               | Auth | Status |
| ------ | ---------------- | ----------------------- | ---- | ------ |
| GET    | `/`              | Listar minhas conversas | Sim  | 200    |
| POST   | `/dm`            | Iniciar conversa direta | Sim  | 201    |
| GET    | `/{id}/messages` | Historico de mensagens  | Sim  | 200    |
| POST   | `/{id}/messages` | Enviar mensagem (REST)  | Sim  | 201    |

#### WebSocket Endpoints (STOMP)

| Destination            | Tipo      | Descricao                                |
| ---------------------- | --------- | ---------------------------------------- |
| `/app/chat.send`       | SEND      | Enviar mensagem                          |
| `/app/chat.typing`     | SEND      | Indicador de digitacao                   |
| `/app/chat.read`       | SEND      | Marcar como lido                         |
| `/topic/chat.{roomId}` | SUBSCRIBE | Receber mensagens, typing, reads do room |

**Exemplos de Request/Response:**

```json
// GET /api/v1/chats
// Response (200):
[
  {
    "id": "ee0e8400-e29b-41d4-a716-446655440060",
    "type": "DM",
    "name": "Luna Starfall",
    "avatarUrl": "https://img.finalfantasyxiv.com/...",
    "lastMessage": {
      "content": "Hey, are you interested in joining our static?",
      "senderName": "Marcus Shield",
      "sentAt": "2026-03-04T20:30:00Z"
    },
    "unreadCount": 2,
    "isActive": true
  },
  {
    "id": "ff0e8400-e29b-41d4-a716-446655440061",
    "type": "STATIC",
    "name": "Heavens' Gate",
    "avatarUrl": null,
    "lastMessage": {
      "content": "Raid tonight at 8 PM EST!",
      "senderName": "Marcus Shield",
      "sentAt": "2026-03-04T18:00:00Z"
    },
    "unreadCount": 0,
    "isActive": true
  }
]
```

```json
// POST /api/v1/chats/dm
// Request:
{
  "targetUserId": "550e8400-e29b-41d4-a716-446655440000"
}

// Response (201):
{
  "id": "ee0e8400-...",
  "type": "DM",
  "name": "Luna Starfall",
  "staticId": null,
  "members": [
    {
      "userId": "550e8400-...",
      "displayName": "Luna Starfall",
      "avatarUrl": "https://...",
      "isOnline": true,
      "lastSeen": null
    },
    {
      "userId": "660e8400-...",
      "displayName": "Marcus Shield",
      "avatarUrl": "https://...",
      "isOnline": true,
      "lastSeen": null
    }
  ],
  "lastMessage": null,
  "unreadCount": 0,
  "isActive": true,
  "createdAt": "2026-03-04T21:00:00Z"
}
```

```json
// WebSocket STOMP: mensagem recebida via /topic/chat.{roomId}
{
  "type": "MESSAGE",
  "data": {
    "id": "110e8400-...",
    "chatRoomId": "ee0e8400-...",
    "senderId": "550e8400-...",
    "senderName": "Luna Starfall",
    "senderAvatar": "https://...",
    "content": "Thanks for the invite! I'd love to join.",
    "sentAt": "2026-03-04T21:05:00Z",
    "editedAt": null,
    "isDeleted": false,
    "reads": []
  }
}
```

### 2.6 Seguranca e Autorizacao

```java
// Todos os endpoints REST requerem autenticacao
// WebSocket: JWT validado no CONNECT frame via ChannelInterceptor
// Verificacao de membership: usuario so pode acessar chat rooms dos quais e membro
// Bloqueio: usuarios bloqueados nao podem enviar DMs (RN41)
```

### 2.7 Validacoes

| Campo          | Regra                                              | Contexto                           |
| -------------- | -------------------------------------------------- | ---------------------------------- |
| `content`      | Obrigatorio, max 4000 chars, sanitizado contra XSS | SendMessageRequest, ChatMessageDto |
| `targetUserId` | Deve existir, nao pode ser o proprio usuario       | CreateDmRequest                    |
| Bloqueio       | Target nao bloqueou o sender (RN41)                | ChatService.createDm               |
| Limite DMs     | Max 20 ativas para free (RN39)                     | ChatService.createDm               |
| Membership     | Remetente deve ser membro ativo do chat room       | MessageService                     |
| Chat ativo     | Chat room deve estar ativo                         | MessageService                     |

### 2.8 Cache (Redis)

| Chave                           | Tipo          | TTL   | Descricao                                     | Invalidacao               |
| ------------------------------- | ------------- | ----- | --------------------------------------------- | ------------------------- |
| `ws:chat:{roomId}`              | Pub/Sub       | -     | Canal de pub/sub para mensagens em tempo real | - (streaming)             |
| `ws:presence:{userId}`          | String        | 5 min | Status online (heartbeat)                     | Disconnect, timeout       |
| `count:chats:{userId}`          | Counter       | -     | Contagem de DMs ativas (limite free - RN39)   | Criacao/desativacao de DM |
| `chat:rooms:{userId}`           | String (JSON) | 1 min | Cache da lista de chat rooms                  | Nova mensagem, novo chat  |
| `chat:unread:{userId}:{roomId}` | Counter       | -     | Contagem de nao lidas por room                | markAsRead                |

### 2.9 Jobs Agendados

Nao ha jobs especificos para este modulo. A limpeza de mensagens antigas poderia ser implementada no CleanupJob (futuro).

---

## 3. Frontend

### 3.1 Paginas e Rotas

| Rota                         | Renderizacao | Descricao                    |
| ---------------------------- | ------------ | ---------------------------- |
| `/[locale]/(main)/chat`      | CSR          | Lista de conversas           |
| `/[locale]/(main)/chat/[id]` | CSR          | Conversa ativa com mensagens |

### 3.2 Componentes React

```
chat/page.tsx                       -- Client Component
  ChatLayout                        -- Layout split (lista + conversa)
    ChatRoomList                     -- Sidebar com conversas
      ChatRoomItem                   -- Item de conversa
        Avatar                       -- Avatar do outro usuario ou static
        LastMessage                  -- Preview da ultima mensagem
        UnreadBadge                  -- Badge de nao lidas
        OnlineIndicator              -- Ponto verde de presenca
      NewDmButton                    -- Botao de nova DM
    ChatConversation                 -- Area de conversa
      ChatHeader                     -- Header com nome e presenca
        RecipientInfo                -- Info do destinatario
        OnlineStatus                 -- Status online/offline
        MembersButton (static)       -- Lista de membros
      MessageList                    -- Lista de mensagens (scroll infinito)
        MessageBubble                -- Bolha de mensagem
          SenderInfo                 -- Avatar e nome do remetente
          MessageContent             -- Conteudo da mensagem
          MessageTimestamp           -- Hora de envio
          ReadReceipts               -- Indicadores de leitura
          EditedIndicator            -- Indicador de edicao
        DateSeparator                -- Separador de data
        SystemMessage                -- Mensagem de sistema
      TypingIndicator                -- "User is typing..."
      MessageInput                   -- Input de mensagem
        TextArea                     -- Campo de texto
        SendButton                   -- Botao de enviar
        EmojiPicker (premium)        -- Seletor de emoji
```

### 3.3 Estado e Data Fetching

#### Zustand Store (chat-store.ts)

```typescript
interface ChatState {
  activeChatRoomId: string | null;
  typingUsers: Record<string, string[]>; // roomId -> userIds typing
  onlineUsers: Set<string>;

  setActiveChatRoom: (roomId: string | null) => void;
  addTypingUser: (roomId: string, userId: string) => void;
  removeTypingUser: (roomId: string, userId: string) => void;
  setUserOnline: (userId: string) => void;
  setUserOffline: (userId: string) => void;
}
```

#### Hooks TanStack Query

```typescript
// hooks/use-chat.ts

function useChatRooms() {
  return useQuery({
    queryKey: ["chats"],
    queryFn: () => api.get("/chats"),
    enabled: authStore.isAuthenticated,
    refetchInterval: 30_000, // refetch a cada 30s como fallback
  });
}

function useChatMessages(roomId: string, page: number = 0) {
  return useInfiniteQuery({
    queryKey: ["chats", roomId, "messages"],
    queryFn: ({ pageParam }) => api.get(`/chats/${roomId}/messages?page=${pageParam}&size=50`),
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
    initialPageParam: 0,
    enabled: !!roomId,
  });
}

function useCreateDm() {
  return useMutation({
    mutationFn: (data: CreateDmRequest) => api.post("/chats/dm", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["chats"] });
    },
  });
}

function useSendMessage() {
  return useMutation({
    mutationFn: ({ roomId, data }: { roomId: string; data: SendMessageRequest }) =>
      api.post(`/chats/${roomId}/messages`, data),
    onSuccess: (_, { roomId }) => {
      queryClient.invalidateQueries({ queryKey: ["chats", roomId, "messages"] });
      queryClient.invalidateQueries({ queryKey: ["chats"] });
    },
  });
}
```

#### Hook useWebSocket

```typescript
// hooks/use-websocket.ts

function useWebSocket() {
  const { accessToken } = useAuthStore();

  useEffect(() => {
    if (!accessToken) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_URL}?token=${accessToken}`),
      onConnect: () => {
        // Subscribe to personal notifications
        client.subscribe("/user/queue/notifications", handleNotification);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.activate();
    return () => client.deactivate();
  }, [accessToken]);
}

// hooks/use-chat-websocket.ts
function useChatWebSocket(roomId: string) {
  const stompClient = useStompClient();

  useEffect(() => {
    if (!stompClient || !roomId) return;

    const subscription = stompClient.subscribe(`/topic/chat.${roomId}`, (message) => {
      const data = JSON.parse(message.body);

      switch (data.type) {
        case "MESSAGE":
          // Adiciona mensagem ao cache do TanStack Query
          queryClient.setQueryData(["chats", roomId, "messages"], (old) => ({
            ...old,
            pages: old.pages.map((page, i) =>
              i === 0 ? { ...page, content: [data.data, ...page.content] } : page,
            ),
          }));
          break;
        case "TYPING":
          if (data.isTyping) {
            chatStore.addTypingUser(roomId, data.userId);
          } else {
            chatStore.removeTypingUser(roomId, data.userId);
          }
          break;
        case "READ":
          queryClient.invalidateQueries({ queryKey: ["chats", roomId, "messages"] });
          break;
      }
    });

    return () => subscription.unsubscribe();
  }, [stompClient, roomId]);

  const sendMessage = (content: string) => {
    stompClient?.publish({
      destination: "/app/chat.send",
      body: JSON.stringify({ chatRoomId: roomId, content }),
    });
  };

  const sendTyping = (isTyping: boolean) => {
    stompClient?.publish({
      destination: "/app/chat.typing",
      body: JSON.stringify({ chatRoomId: roomId, isTyping }),
    });
  };

  const markRead = (lastReadMessageId: string) => {
    stompClient?.publish({
      destination: "/app/chat.read",
      body: JSON.stringify({ chatRoomId: roomId, lastReadMessageId }),
    });
  };

  return { sendMessage, sendTyping, markRead };
}
```

#### Hook usePresence

```typescript
// hooks/use-presence.ts
function usePresence() {
  // Heartbeat a cada 2 minutos para manter presenca
  useEffect(() => {
    const interval = setInterval(() => {
      api.post("/presence/heartbeat");
    }, 120_000);
    return () => clearInterval(interval);
  }, []);
}
```

### 3.4 Formularios e Validacao (Zod)

```typescript
// schemas/chat.ts
import { z } from "zod";

export const sendMessageSchema = z.object({
  content: z.string().min(1, "Message cannot be empty").max(4000),
});

export const createDmSchema = z.object({
  targetUserId: z.string().uuid(),
});
```

### 3.5 WebSocket

| Destination            | Tipo      | Descricao                                               |
| ---------------------- | --------- | ------------------------------------------------------- |
| `/app/chat.send`       | SEND      | Enviar mensagem no chat room                            |
| `/app/chat.typing`     | SEND      | Enviar indicador de digitacao                           |
| `/app/chat.read`       | SEND      | Marcar mensagens como lidas                             |
| `/topic/chat.{roomId}` | SUBSCRIBE | Receber eventos do chat room (mensagens, typing, reads) |

### 3.6 Internacionalizacao

**Namespaces i18n:**

| Namespace       | Uso                        |
| --------------- | -------------------------- |
| `chat.*`        | Pagina de chat             |
| `chat.dm.*`     | Mensagens diretas          |
| `chat.static.*` | Chat de static             |
| `chat.typing.*` | Indicadores de digitacao   |
| `chat.empty.*`  | Estados vazios             |
| `chat.limit.*`  | Mensagens de limite (free) |

---

## 4. Banco de Dados

### 4.1 DDL

```sql
-- Migration: V7__create_chat.sql

CREATE TABLE chat_rooms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type        VARCHAR(20) NOT NULL, -- DM, STATIC
    static_id   UUID REFERENCES statics(id) ON DELETE CASCADE,
    name        VARCHAR(100),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_room_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id    UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at         TIMESTAMPTZ,
    last_read_at    TIMESTAMPTZ,
    is_muted        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_chat_member UNIQUE (chat_room_id, user_id)
);

CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id    UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users(id),
    content         VARCHAR(4000) NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    edited_at       TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE message_reads (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id  UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    read_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_message_read UNIQUE (message_id, user_id)
);

-- Indices
CREATE INDEX idx_chat_rooms_static ON chat_rooms(static_id) WHERE static_id IS NOT NULL;
CREATE INDEX idx_chat_rooms_type ON chat_rooms(type, is_active);
CREATE INDEX idx_chat_members_room ON chat_room_members(chat_room_id) WHERE left_at IS NULL;
CREATE INDEX idx_chat_members_user ON chat_room_members(user_id) WHERE left_at IS NULL;
CREATE INDEX idx_messages_room_sent ON messages(chat_room_id, sent_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_message_reads_message ON message_reads(message_id);
CREATE INDEX idx_message_reads_user ON message_reads(user_id);
```

### 4.2 Indices

| Tabela              | Indice                  | Tipo                 | Justificativa                    |
| ------------------- | ----------------------- | -------------------- | -------------------------------- |
| `chat_rooms`        | `static_id`             | BTREE (partial)      | Chat da static                   |
| `chat_room_members` | `chat_room_id`          | BTREE (partial)      | Membros ativos do chat           |
| `chat_room_members` | `user_id`               | BTREE (partial)      | Chats do usuario                 |
| `chat_room_members` | `chat_room_id, user_id` | UNIQUE               | Um membro por chat               |
| `messages`          | `chat_room_id, sent_at` | BTREE DESC (partial) | Historico de mensagens           |
| `message_reads`     | `message_id, user_id`   | UNIQUE               | Uma leitura por mensagem/usuario |

---

## 5. Integracao Entre Modulos

### 5.1 Dependencias

```
chat
    |
    +--> auth-profile (User para membros e remetentes)
    |
    +--> premium (verifica limite de DMs - RN39)
    |
    <-- static-management (StaticCreatedEvent, MemberJoinedEvent, MemberLeftEvent, StaticDissolvedEvent)
    |
    <-- auth-profile (UserBlockedEvent para desativar DMs)
```

---

## 6. Tratamento de Erros

| HTTP | Tipo RFC 7807                 | Cenario                          |
| ---- | ----------------------------- | -------------------------------- |
| 400  | `/errors/validation`          | Campos invalidos                 |
| 400  | `/errors/empty-message`       | Mensagem vazia                   |
| 403  | `/errors/not-chat-member`     | Nao e membro do chat room        |
| 403  | `/errors/user-blocked`        | Usuario bloqueado (RN41)         |
| 403  | `/errors/premium-required`    | Limite de DMs atingido (RN39)    |
| 404  | `/errors/chat-room-not-found` | Chat room nao encontrado         |
| 404  | `/errors/user-not-found`      | Usuario alvo nao encontrado      |
| 409  | `/errors/dm-already-exists`   | DM ja existe entre os usuarios   |
| 409  | `/errors/chat-room-inactive`  | Chat room esta desativado        |
| 409  | `/errors/self-dm`             | Nao pode enviar DM para si mesmo |
