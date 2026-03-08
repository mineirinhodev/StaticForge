# FFXIV Static Planner - Documento de Area de Negocio

## 1. Visao Geral do Produto

### 1.1 Problema

No Final Fantasy XIV, a formacao e manutencao de **statics** (grupos fixos de raid) e um dos maiores desafios da comunidade endgame. Jogadores enfrentam:

- **Dificuldade em encontrar membros compativeis**: horarios, server, nivel de progressao, roles e expectativas precisam se alinhar simultaneamente.
- **Falta de ferramentas centralizadas**: recrutamento acontece em canais fragmentados (Discord, Reddit, Party Finder in-game, forums), sem padronizacao.
- **Ausencia de historico e reputacao**: nao existe forma confiavel de avaliar a seriedade e o comportamento de jogadores antes de recruta-los.
- **Gestao manual de agendas**: lideres de static coordenam horarios por planilhas ou mensagens, sem automacao.
- **Barreira linguistica**: jogadores de diferentes paises compartilham Data Centers, mas nao ha ferramentas multilinguais para conecta-los.

### 1.2 Solucao

O **FFXIV Static Planner** e uma plataforma web dedicada ao recrutamento, formacao e gestao de statics no Final Fantasy XIV. Oferece:

- Sistema bidirecional de recrutamento (statics buscam membros e jogadores buscam statics)
- Matching automatico avancado baseado em multiplos criterios
- Calendario integrado com suporte a PUG (Pick-Up Groups)
- Sistema de reputacao publico
- Integracao com o Lodestone (perfil oficial do FFXIV)
- Chat interno para comunicacao direta
- Suporte multilingual

### 1.3 Proposta de Valor

| Para | Valor |
|------|-------|
| **Jogadores individuais** | Encontrar statics compativeis com seu perfil, horario e objetivos sem depender de canais dispersos |
| **Lideres de static** | Recrutar membros qualificados, gerenciar agenda da equipe e manter historico organizado |
| **Statics em formacao** | Preencher vagas rapidamente com jogadores compativeis via matching inteligente |
| **Comunidade** | Sistema de reputacao transparente que eleva a qualidade das interacoes entre jogadores |

---

## 2. Modelo de Negocio - Freemium

### 2.1 Plano Gratuito (Free)

Recursos disponiveis para todos os usuarios sem custo:

| Categoria | Recursos |
|-----------|----------|
| **Perfil** | Criacao de perfil, vinculo com Lodestone, definicao de jobs/roles, disponibilidade horaria |
| **Static** | Criar 1 static, participar de statics ilimitadas como membro |
| **Busca** | Busca basica por statics e jogadores com filtros padrao |
| **Matching** | Receber ate 5 sugestoes de match por semana |
| **Calendario** | Visualizar calendario da static, confirmar presenca, ver eventos publicos |
| **Reputacao** | Visualizar e receber avaliacoes, ver reputacao de outros jogadores |
| **Chat** | Mensagens diretas (limite de 20 conversas ativas), chat da static |
| **Notificacoes** | Notificacoes basicas (in-app) |

### 2.2 Plano Premium

Recursos adicionais pagos via assinatura mensal/anual:

| Categoria | Recursos Premium |
|-----------|-----------------|
| **Perfil** | Badge premium visivel, perfil destacado nos resultados de busca, estatisticas avancadas de perfil |
| **Static** | Criar statics ilimitadas, pagina personalizada da static com banner/descricao rica |
| **Busca** | Filtros avancados (por parse, clear times, historico de raids), busca por afinidade |
| **Matching** | Sugestoes de match ilimitadas, matching prioritario (aparecer primeiro nos resultados), filtros de match avancados |
| **Calendario** | Criar eventos publicos para PUG, lembretes personalizados (push/email/discord webhook), exportar calendario (iCal) |
| **Reputacao** | Selo de verificacao, historico completo de avaliacoes, analytics de reputacao |
| **Chat** | Conversas ilimitadas, envio de imagens/links, historico de mensagens completo |
| **Notificacoes** | Notificacoes push, email digests, webhooks para Discord |
| **Suporte** | Suporte prioritario, acesso antecipado a novos recursos |

### 2.3 Estrategia de Monetizacao

- **Modelo de assinatura**: mensal e anual (desconto no anual)
- **Sem pay-to-win**: recursos premium oferecem conveniencia e visibilidade, nunca vantagem funcional excludente
- **Trial premium**: 7 dias gratis para novos usuarios experimentarem recursos premium
- **Conversao organica**: o valor do matching avancado e visibilidade incentivam upgrade natural

---

## 3. Stakeholders e Atores do Sistema

### 3.1 Atores Primarios

| Ator | Descricao |
|------|-----------|
| **Jogador (Player)** | Usuario individual que busca uma static, define seu perfil de jogo e disponibilidade |
| **Lider de Static (Static Leader)** | Jogador que cria e gerencia uma static, define vagas e requisitos |
| **Membro de Static (Static Member)** | Jogador que participa de uma static existente com role definida |

### 3.2 Atores Secundarios

| Ator | Descricao |
|------|-----------|
| **Sistema de Matching** | Servico automatizado que cruza dados de jogadores e statics para gerar sugestoes |
| **Lodestone API** | Servico externo do Square Enix que fornece dados do personagem |
| **Sistema de Notificacoes** | Servico que envia alertas via in-app, email, push e webhooks |
| **Administrador da Plataforma** | Equipe que modera conteudo, resolve disputas e gerencia a plataforma |
| **Sistema de Pagamentos** | Gateway de pagamento para assinaturas premium |

### 3.3 Stakeholders

| Stakeholder | Interesse |
|-------------|-----------|
| **Equipe de desenvolvimento** | Construir e manter a plataforma |
| **Comunidade FFXIV** | Ter acesso a ferramenta que facilite o endgame social |
| **Square Enix** | Indiretamente interessada em ferramentas que enriquecem a experiencia do jogo (respeitando ToS) |

---

## 4. Personas

### 4.1 Persona 1 - Luna (Jogadora Casual-to-Midcore)

| Atributo | Detalhe |
|----------|---------|
| **Idade** | 27 anos |
| **Servidor** | Lamia (NA - Primal) |
| **Jobs** | WHM (main), AST (alt) |
| **Disponibilidade** | Terca e quinta, 20h-23h EST |
| **Progressao** | Cleared P9S-P11S, progging P12S |
| **Idiomas** | Ingles, Espanhol |
| **Objetivo** | Encontrar uma static casual-midcore que raida 2x por semana e respeite horarios |
| **Frustracao** | Ja passou por 3 statics que disbandaram por falta de organizacao |
| **Motivacao** | Quer um grupo estavel com boa comunicacao e calendarioorganizado |

### 4.2 Persona 2 - Marcus (Lider de Static Hardcore)

| Atributo | Detalhe |
|----------|---------|
| **Idade** | 32 anos |
| **Servidor** | Ragnarok (EU - Chaos) |
| **Jobs** | PLD/GNB (main tank) |
| **Disponibilidade** | Segunda a quinta, 19h-23h CET |
| **Progressao** | Week 1-2 clears, parsing 90+ |
| **Idiomas** | Ingles, Alemao |
| **Objetivo** | Recrutar DPS com parses altos e comprometimento para clear de primeira semana |
| **Frustracao** | Dificil verificar se candidatos realmente tem os parses que dizem ter |
| **Motivacao** | Quer ferramentas para filtrar candidatos por dados verificaveis e reputacao |

### 4.3 Persona 3 - Yuki (Jogadora Newcomer)

| Atributo | Detalhe |
|----------|---------|
| **Idade** | 22 anos |
| **Servidor** | Tonberry (JP - Elemental) |
| **Jobs** | SAM (unico job 90) |
| **Disponibilidade** | Fins de semana, flexivel |
| **Progressao** | Zerou MSQ, nunca fez Savage |
| **Idiomas** | Japones, Ingles basico |
| **Objetivo** | Encontrar uma static para aprendizado de Savage que aceite iniciantes |
| **Frustracao** | Intimidade para se candidatar em grupos experientes, barreira linguistica |
| **Motivacao** | Quer um ambiente acolhedor para aprender mecanicas de raid |

### 4.4 Persona 4 - Rafael (PUG Organizer)

| Atributo | Detalhe |
|----------|---------|
| **Idade** | 29 anos |
| **Servidor** | Behemoth (NA - Primal) |
| **Jobs** | SCH/SGE (healers), RPR (DPS alt) |
| **Disponibilidade** | Irregular, mas frequente |
| **Progressao** | Todos os Savage cleared, faz reclears semanais |
| **Idiomas** | Ingles, Portugues |
| **Objetivo** | Organizar PUGs de reclear semanais e encontrar jogadores confiaveis para farm |
| **Frustracao** | Party Finder nao permite agendar com antecedencia nem filtrar por reputacao |
| **Motivacao** | Quer calendario publico para PUGs com sistema de confirmacao e reputacao |

---

## 5. Glossario de Termos FFXIV

| Termo | Definicao |
|-------|-----------|
| **Static** | Grupo fixo de jogadores (geralmente 8) que se reunem regularmente para fazer conteudo de raid |
| **Raid** | Conteudo de combate cooperativo de alta dificuldade, requerendo coordenacao entre multiplos jogadores |
| **Savage** | Nivel de dificuldade alto de raids com mecanicas mais complexas e DPS checks mais apertados |
| **Ultimate** | Nivel de dificuldade maximo de raids, exigindo execucao quase perfeita |
| **Extreme (EX)** | Trials de dificuldade intermediaria, mais dificeis que Normal mas abaixo de Savage |
| **Criterion / Variant** | Dungeons de dificuldade variavel para 4 jogadores |
| **Role** | Funcao do jogador no grupo: Tank, Healer ou DPS |
| **Job** | Classe especifica do personagem (ex: Paladin, White Mage, Samurai) |
| **Main** | Job principal que o jogador mais usa e tem mais experiencia |
| **Alt (job)** | Job secundario que o jogador consegue jogar com competencia |
| **Parse** | Pontuacao de performance medida por ferramentas como FFLogs (percentil 0-100) |
| **Prog / Progression** | O ato de progredir atraves das mecanicas de um fight, avancando cada vez mais |
| **Clear** | Derrotar o boss de um encounter pela primeira vez |
| **Reclear** | Derrotar novamente um boss ja cleared para obter loot semanal |
| **Farm** | Repetir conteudo ja dominado para obter loot ou materiais |
| **PUG (Pick-Up Group)** | Grupo formado por jogadores que nao fazem parte da mesma static, geralmente via Party Finder |
| **Party Finder (PF)** | Sistema in-game para recrutar jogadores para conteudo |
| **Disband** | Dissolucao de uma static |
| **Raid Night** | Sessao agendada de raid da static |
| **Loot Rules** | Regras da static para distribuicao de itens obtidos nas raids |
| **World / Server** | Servidor onde o personagem existe |
| **Data Center (DC)** | Agrupamento de servidores que podem jogar juntos |
| **Lodestone** | Site oficial do FFXIV onde cada personagem tem um perfil publico |
| **FFLogs** | Site de terceiros que registra e analisa performance em combate |
| **Enrage** | Timer final de um encounter - se o boss nao morrer antes, o grupo e eliminado |
| **Mechanic** | Padrao de ataque/evento em um encounter que exige resposta especifica dos jogadores |
| **Uptime** | Estrategia que maximiza o tempo de ataque ao boss durante mecanicas |
| **Mitigation** | Habilidades defensivas usadas para reduzir dano recebido |
| **Buff Window** | Janela de tempo onde buffs do grupo se acumulam para maximizar dano |
| **iLvl (Item Level)** | Nivel medio dos equipamentos do personagem, indicando poder geral |

---

## 6. Regras de Negocio

### 6.1 Perfil e Conta

| ID | Regra |
|----|-------|
| **RN01** | Um usuario pode ter multiplos personagens vinculados, mas deve definir um como principal |
| **RN02** | A verificacao via Lodestone e obrigatoria para ativar funcionalidades de matching e reputacao |
| **RN03** | O perfil deve conter ao menos: 1 job definido, disponibilidade horaria e Data Center |
| **RN04** | Cada personagem so pode ser vinculado a uma unica conta na plataforma |
| **RN05** | O idioma preferido do usuario e definido no perfil e afeta a interface e as sugestoes de matching |

### 6.2 Static

| ID | Regra |
|----|-------|
| **RN06** | Uma static deve ter entre 2 e 8 membros (padrao de 8 para full party de raid) |
| **RN07** | Cada static tem exatamente 1 lider. A lideranca pode ser transferida para outro membro |
| **RN08** | O lider define: composicao desejada (roles/jobs), horarios de raid, nivel de conteudo, regras de loot e idioma |
| **RN09** | Uma static pode definir escopo customizavel de conteudo (Savage, Ultimate, Extreme, Criterion, etc.) |
| **RN10** | Usuarios free podem criar ate 1 static; usuarios premium podem criar statics ilimitadas |
| **RN11** | Um jogador pode ser membro de multiplas statics simultaneamente |
| **RN12** | Quando uma static e dissolvida, todos os membros recebem notificacao e o historico e preservado |

### 6.3 Recrutamento Bidirecional

| ID | Regra |
|----|-------|
| **RN13** | Statics podem publicar vagas com requisitos especificos (job, experiencia, parse, horario) |
| **RN14** | Jogadores podem se candidatar a vagas de statics |
| **RN15** | Statics podem convidar jogadores diretamente para preencher vagas |
| **RN16** | Jogadores podem publicar perfil como "Buscando Static" (LFS - Looking for Static) |
| **RN17** | Uma candidatura pode ser: Pendente, Aceita, Recusada ou Expirada (expira em 14 dias sem resposta) |
| **RN18** | Um convite pode ser: Pendente, Aceito, Recusado ou Expirado (expira em 7 dias sem resposta) |

### 6.4 Matching

| ID | Regra |
|----|-------|
| **RN19** | O matching considera: role/job, horario, Data Center, idioma, nivel de progressao, tipo de conteudo |
| **RN20** | Usuarios free recebem ate 5 sugestoes por semana; premium recebem ilimitadas |
| **RN21** | O score de match e calculado com peso configuravel para cada criterio |
| **RN22** | Matches com compatibilidade abaixo de 40% nao sao exibidos |
| **RN23** | O sistema prioriza matches dentro do mesmo Data Center (cross-DC e possivel mas com menor peso) |

### 6.5 Calendario

| ID | Regra |
|----|-------|
| **RN24** | Cada static tem um calendario compartilhado visivel para todos os membros |
| **RN25** | Eventos de raid exigem confirmacao de presenca: Confirmado, Tentativo ou Ausente |
| **RN26** | Eventos publicos (PUG) sao visiveis para qualquer usuario do mesmo Data Center |
| **RN27** | Eventos PUG tem vagas limitadas com sistema de inscricao e lista de espera |
| **RN28** | Lembretes sao enviados automaticamente: 24h e 1h antes do evento (configuravel para premium) |
| **RN29** | Se um membro marca Ausente, o lider recebe notificacao para buscar substituto |

### 6.6 Reputacao

| ID | Regra |
|----|-------|
| **RN30** | Avaliacoes sao bidirecionais: membros avaliam a static e a static avalia o membro |
| **RN31** | Avaliacoes sao publicas e visiveis no perfil do jogador e da static |
| **RN32** | Uma avaliacao so pode ser feita apos minimo de 2 semanas de convivencia na static |
| **RN33** | Criterios de avaliacao: pontualidade, comunicacao, performance, atitude, comprometimento |
| **RN34** | Avaliacoes podem ser reportadas por abuso; administradores moderam disputas |
| **RN35** | A nota de reputacao e uma media ponderada das avaliacoes (avaliacoes mais recentes tem mais peso) |
| **RN36** | Jogadores com reputacao abaixo de 2.0/5.0 recebem alerta e podem ter restricoes de matching |

### 6.7 Chat

| ID | Regra |
|----|-------|
| **RN37** | Mensagens diretas sao permitidas entre qualquer par de usuarios da plataforma |
| **RN38** | Cada static tem um chat de grupo automatico para seus membros |
| **RN39** | Usuarios free tem limite de 20 conversas diretas ativas; premium ilimitado |
| **RN40** | Mensagens podem ser reportadas; acumulode reports leva a restricao da conta |
| **RN41** | Usuarios podem bloquear outros usuarios, impedindo mensagens e matches |

### 6.8 Premium

| ID | Regra |
|----|-------|
| **RN42** | A assinatura premium pode ser mensal ou anual (com desconto) |
| **RN43** | Cancelamento de premium mantem os recursos ativos ate o fim do periodo pago |
| **RN44** | Ao expirar premium: statics extras ficam em modo somente leitura, matches voltam ao limite free |
| **RN45** | Existe trial de 7 dias para novos usuarios, ativavel uma unica vez por conta |

---

## 7. Modelo de Dominio

### 7.1 Entidades Principais

```
+------------------+        +------------------+        +------------------+
|      User        |        |    Character     |        |     Static       |
|------------------|        |------------------|        |------------------|
| id               |1    N  | id               |  N   1 | id               |
| email            |------->| name             |<-------| name             |
| passwordHash     |        | server           |        | description      |
| locale           |        | dataCenter       |        | contentScope[]   |
| isPremium        |        | lodestoneId      |        | lootRules        |
| premiumExpiresAt |        | isVerified       |        | language         |
| createdAt        |        | isMain           |        | status           |
| updatedAt        |        | avatarUrl        |        | maxMembers       |
|                  |        | updatedAt        |        | createdAt        |
+------------------+        +------------------+        +------------------+
                                    |                          |
                                    |1                         |1
                                    |                          |
                            +------------------+        +------------------+
                            |   CharacterJob   |        | StaticMembership |
                            |------------------|        |------------------|
                            | id               |        | id               |
                            | characterId      |        | staticId         |
                            | jobName          |        | characterId      |
                            | role (T/H/D)     |        | role             |
                            | isMain           |        | jobAssigned      |
                            | ilvl             |        | isLeader         |
                            | parseTier        |        | joinedAt         |
                            | progression      |        | leftAt           |
                            +------------------+        +------------------+
```

```
+------------------+        +------------------+        +------------------+
|  StaticVacancy   |        |   Application    |        |    Invitation    |
|------------------|        |------------------|        |------------------|
| id               |        | id               |        | id               |
| staticId         |        | vacancyId        |        | staticId         |
| roleNeeded       |        | characterId      |        | characterId      |
| jobPreference[]  |        | message          |        | message          |
| requirements     |        | status           |        | status           |
| description      |        | appliedAt        |        | sentAt           |
| isOpen           |        | respondedAt      |        | respondedAt      |
| createdAt        |        +------------------+        +------------------+
+------------------+
```

```
+------------------+        +------------------+        +------------------+
|   RaidEvent      |        |   Attendance     |        |   MatchSuggest   |
|------------------|        |------------------|        |------------------|
| id               |        | id               |        | id               |
| staticId (null=  |        | eventId          |        | characterId      |
|   PUG)           |        | characterId      |        | staticId/charId  |
| title            |        | status           |        | score            |
| description      |        | respondedAt      |        | criteria{}       |
| startTime        |        +------------------+        | status           |
| endTime          |                                    | createdAt        |
| timezone         |                                    +------------------+
| contentType      |
| isPublic         |
| maxParticipants  |
| createdBy        |
+------------------+
```

```
+------------------+        +------------------+        +------------------+
|    Review        |        |    ChatRoom      |        |    Message       |
|------------------|        |------------------|        |------------------|
| id               |        | id               |        | id               |
| reviewerId       |        | type (DM/STATIC) |        | chatRoomId       |
| targetId         |        | staticId (null=  |        | senderId         |
| targetType       |        |   DM)            |        | content          |
| (PLAYER/STATIC)  |        | createdAt        |        | sentAt           |
| punctuality      |        +------------------+        | readBy[]         |
| communication    |                                    +------------------+
| performance      |
| attitude         |
| commitment       |
| comment          |
| createdAt        |
+------------------+
```

```
+------------------+        +------------------+
|  Subscription    |        |   Notification   |
|------------------|        |------------------|
| id               |        | id               |
| userId           |        | userId           |
| plan             |        | type             |
| status           |        | title            |
| startDate        |        | body             |
| endDate          |        | link             |
| paymentMethod    |        | isRead           |
| autoRenew        |        | createdAt        |
+------------------+        +------------------+
```

### 7.2 Relacionamentos Principais

| Relacionamento | Cardinalidade | Descricao |
|---------------|---------------|-----------|
| User -> Character | 1:N | Um usuario pode ter multiplos personagens |
| Character -> CharacterJob | 1:N | Um personagem pode ter multiplos jobs configurados |
| Static -> StaticMembership | 1:N | Uma static tem multiplos membros (max 8) |
| Character -> StaticMembership | 1:N | Um personagem pode estar em multiplas statics |
| Static -> StaticVacancy | 1:N | Uma static pode ter multiplas vagas abertas |
| StaticVacancy -> Application | 1:N | Uma vaga pode receber multiplas candidaturas |
| Static -> Invitation | 1:N | Uma static pode enviar multiplos convites |
| Static -> RaidEvent | 1:N | Uma static tem multiplos eventos no calendario |
| RaidEvent -> Attendance | 1:N | Um evento tem multiplas confirmacoes de presenca |
| Character -> Review | 1:N (ambos lados) | Um personagem pode dar e receber avaliacoes |
| ChatRoom -> Message | 1:N | Uma sala de chat tem multiplas mensagens |
| User -> Subscription | 1:1 | Um usuario tem no maximo uma assinatura ativa |
| User -> Notification | 1:N | Um usuario recebe multiplas notificacoes |

---

## 8. Integracoes Externas

### 8.1 Lodestone (FFXIV Official)

A integracao com o Lodestone e o pilar de verificacao de identidade da plataforma.

#### Funcionalidades

| Funcionalidade | Descricao |
|----------------|-----------|
| **Verificacao de personagem** | Confirmar que o usuario e dono do personagem FFXIV informado |
| **Importacao de dados** | Obter automaticamente: nome, servidor, Data Center, raca, genero, avatar, Free Company |
| **Sincronizacao de jobs** | Importar niveis de jobs e equipamentos do personagem |
| **Atualizacao periodica** | Manter dados do personagem atualizados via consultas periodicas |

#### Processo de Verificacao

1. Usuario informa o nome do personagem e servidor
2. Sistema busca o perfil no Lodestone e exibe para confirmacao
3. Usuario adiciona um codigo de verificacao unico no campo "Bio" do Lodestone
4. Sistema confirma a presenca do codigo e marca o personagem como verificado
5. Usuario pode remover o codigo da bio apos verificacao

#### Limitacoes

- O Lodestone nao possui API oficial documentada; a integracao depende de scraping ou APIs comunitarias (ex: XIVAPI)
- Dados de parse/FFLogs nao vem do Lodestone (sao auto-declarados ou integrados separadamente)
- Respeitar rate limits para evitar bloqueio
- Dados podem ter delay de atualizacao

### 8.2 Integracoes Futuras (Roadmap)

| Integracao | Prioridade | Descricao |
|-----------|------------|-----------|
| **FFLogs** | Alta | Verificar parses e historico de clears automaticamente |
| **Discord** | Alta | Webhooks para notificacoes, vinculo de conta para contato |
| **Google Calendar / iCal** | Media | Exportar eventos de raid para calendarios pessoais |
| **Twitch** | Baixa | Vincular stream ao perfil para prog streams |

---

## 9. Requisitos de Multilinguismo

### 9.1 Estrategia

| Aspecto | Abordagem |
|---------|-----------|
| **Framework** | Internacionalizacao (i18n) com chaves de traducao para toda a interface |
| **Idiomas iniciais** | Ingles (padrao), Japones, Alemao, Frances, Portugues |
| **Conteudo de usuario** | Perfis e descricoes ficam no idioma original do autor |
| **Matching por idioma** | O idioma e um criterio de matching com peso configuravel |
| **Deteccao automatica** | O sistema sugere o idioma com base no locale do navegador |
| **Preferencia manual** | O usuario pode trocar o idioma a qualquer momento |

### 9.2 O que e Traduzido

| Elemento | Traduzido? |
|----------|-----------|
| Interface (menus, botoes, labels) | Sim |
| Mensagens de sistema e notificacoes | Sim |
| Nomes de jobs, roles e conteudos FFXIV | Sim (termos oficiais de cada idioma) |
| Descricoes de statics escritas por usuarios | Nao (mantido no idioma original) |
| Mensagens de chat | Nao (mantido no idioma original) |
| Avaliacoes/reviews | Nao (mantido no idioma original) |
| Emails transacionais | Sim (no idioma preferido do usuario) |

### 9.3 Consideracoes por Regiao

| Regiao | Data Centers | Idiomas predominantes |
|--------|-------------|----------------------|
| NA (North America) | Aether, Primal, Crystal, Dynamis | Ingles, Espanhol, Portugues |
| EU (Europe) | Chaos, Light | Ingles, Alemao, Frances |
| JP (Japan) | Elemental, Gaia, Mana, Meteor | Japones, Ingles |
| OCE (Oceania) | Materia | Ingles |

---

## 10. Metricas de Sucesso

| Metrica | Descricao | Meta (Ano 1) |
|---------|-----------|-------------|
| **MAU** | Usuarios ativos mensais | 10.000 |
| **Statics formadas** | Statics criadas e ativas na plataforma | 2.000 |
| **Taxa de match** | % de sugestoes de matching que resultam em candidatura/convite | > 15% |
| **Retencao 30 dias** | % de usuarios que retornam apos 30 dias | > 40% |
| **Conversao premium** | % de usuarios free que convertem para premium | > 5% |
| **NPS** | Net Promoter Score da comunidade | > 50 |
| **Tempo para fill** | Tempo medio para preencher uma vaga de static | < 7 dias |

---

## 11. Restricoes e Premissas

### 11.1 Restricoes

| Restricao | Descricao |
|-----------|-----------|
| **ToS do FFXIV** | A plataforma nao pode violar os Termos de Servico do jogo; dados de parse sao sensibilidade pois ferramentas de DPS meter sao tecnicamente nao permitidas |
| **Lodestone** | Sem API oficial; dependencia de APIs comunitarias |
| **GDPR/LGPD** | Conformidade com leis de protecao de dados (usuarios globais) |
| **Escalabilidade** | Picos de uso coincidem com lancamento de patches e tiers de raid |

### 11.2 Premissas

| Premissa | Descricao |
|----------|-----------|
| A comunidade FFXIV tem demanda por ferramentas de recrutamento mais sofisticadas que o Party Finder |
| Jogadores estao dispostos a vincular seus personagens do Lodestone para verificacao |
| O modelo freemium e sustentavel se os recursos premium agregam valor percebido significativo |
| APIs comunitarias como XIVAPI se manterao disponiveis e funcionais |
