# FFXIV Static Planner - Documento de Casos de Uso

## Indice

- [Modulo 1: Autenticacao & Perfil (UC01-UC04)](#modulo-1-autenticacao--perfil)
- [Modulo 2: Static (UC05-UC11)](#modulo-2-static)
- [Modulo 3: Matching (UC12-UC14)](#modulo-3-matching)
- [Modulo 4: Calendario (UC15-UC19)](#modulo-4-calendario)
- [Modulo 5: Reputacao (UC20-UC22)](#modulo-5-reputacao)
- [Modulo 6: Chat (UC23-UC25)](#modulo-6-chat)
- [Modulo 7: Premium (UC26-UC28)](#modulo-7-premium)

---

## Modulo 1: Autenticacao & Perfil

### UC01 - Registrar Conta

| Campo | Descricao |
|-------|-----------|
| **ID** | UC01 |
| **Nome** | Registrar Conta |
| **Atores** | Jogador (primario), Sistema de Email (secundario) |
| **Pre-condicoes** | Nenhuma |
| **Pos-condicoes** | Conta criada, email de verificacao enviado, usuario redirecionado ao fluxo de criacao de perfil |

**Fluxo Principal:**

1. Jogador acessa a pagina de registro
2. Jogador preenche: email, senha, confirmacao de senha e idioma preferido
3. Sistema valida que o email nao esta em uso
4. Sistema valida requisitos da senha (minimo 8 caracteres, 1 maiuscula, 1 numero)
5. Sistema cria a conta com status "pendente de verificacao"
6. Sistema envia email de verificacao com link unico (expira em 24h)
7. Jogador clica no link de verificacao
8. Sistema ativa a conta e redireciona para UC02 (Criar Perfil)

**Fluxos Alternativos:**

- **FA01 - Registro via OAuth**: Jogador escolhe registro via Google/Discord. Sistema cria conta vinculada ao provedor OAuth, pulando a verificacao de email.
- **FA02 - Reenvio de email**: Jogador nao recebeu o email. Clica em "Reenviar" e sistema envia novo email (limite de 3 reenvios por hora).

**Fluxos de Excecao:**

- **FE01 - Email ja cadastrado**: Sistema informa que o email ja esta em uso e oferece opcao de login ou recuperacao de senha.
- **FE02 - Senha fraca**: Sistema exibe requisitos nao atendidos e solicita nova senha.
- **FE03 - Link expirado**: Sistema informa que o link expirou e oferece reenvio.

---

### UC02 - Criar/Editar Perfil de Jogador

| Campo | Descricao |
|-------|-----------|
| **ID** | UC02 |
| **Nome** | Criar/Editar Perfil de Jogador |
| **Atores** | Jogador (primario) |
| **Pre-condicoes** | Conta ativa e verificada |
| **Pos-condicoes** | Perfil do jogador criado/atualizado com informacoes de jogo |

**Fluxo Principal:**

1. Jogador acessa a pagina de perfil
2. Jogador informa o nome do personagem principal e servidor
3. Sistema busca o personagem no Lodestone (via XIVAPI) e exibe para confirmacao
4. Jogador confirma que o personagem esta correto
5. Sistema importa dados basicos: avatar, raca, servidor, Data Center, Free Company
6. Jogador define seus jobs:
   - Seleciona job(s) principal(is) e alternativo(s)
   - Para cada job, informa: role, nivel de item (iLvl), faixa de parse (opcional), progressao
7. Jogador define disponibilidade horaria:
   - Seleciona dias da semana disponiveis
   - Define faixa de horario para cada dia
   - Define timezone
8. Jogador define preferencias:
   - Tipo de conteudo de interesse (Savage, Ultimate, Extreme, Criterion)
   - Nivel de seriedade (casual, midcore, hardcore)
   - Idiomas falados
   - Se esta "Buscando Static" (LFS) ativamente
9. Sistema salva o perfil

**Fluxos Alternativos:**

- **FA01 - Edicao parcial**: Jogador edita apenas uma secao do perfil sem alterar as demais.
- **FA02 - Adicionar personagem alt**: Jogador adiciona personagens adicionais (alts) repetindo os passos 2-5 para cada.
- **FA03 - Personagem nao encontrado**: Jogador informa manualmente os dados do personagem para completar depois com verificacao Lodestone.

**Fluxos de Excecao:**

- **FE01 - Lodestone indisponivel**: Sistema informa o problema e permite preenchimento manual com aviso de que a verificacao sera necessaria depois.
- **FE02 - Personagem ja vinculado**: Sistema informa que o personagem ja esta vinculado a outra conta.

---

### UC03 - Vincular e Verificar Personagem via Lodestone

| Campo | Descricao |
|-------|-----------|
| **ID** | UC03 |
| **Nome** | Vincular e Verificar Personagem via Lodestone |
| **Atores** | Jogador (primario), Lodestone/XIVAPI (secundario) |
| **Pre-condicoes** | Conta ativa, personagem informado no perfil |
| **Pos-condicoes** | Personagem marcado como "verificado", funcionalidades de matching e reputacao desbloqueadas |

**Fluxo Principal:**

1. Jogador acessa a secao de verificacao no perfil
2. Sistema gera um codigo de verificacao unico (ex: `FFSP-A3X9-K2M1`)
3. Sistema exibe instrucoes: "Acesse o Lodestone, edite seu perfil e adicione o codigo abaixo no campo Bio/Profile"
4. Jogador acessa o Lodestone externamente e adiciona o codigo na bio do personagem
5. Jogador retorna a plataforma e clica em "Verificar Agora"
6. Sistema consulta o Lodestone e busca o codigo na bio do personagem
7. Sistema encontra o codigo e marca o personagem como verificado
8. Sistema importa/atualiza dados completos do personagem
9. Sistema informa que o jogador pode remover o codigo da bio do Lodestone

**Fluxos Alternativos:**

- **FA01 - Verificacao por personagem alt**: Jogador repete o processo para personagens secundarios.
- **FA02 - Re-verificacao**: Apos mudanca de servidor (transfer), o jogador precisa re-verificar.

**Fluxos de Excecao:**

- **FE01 - Codigo nao encontrado**: Sistema informa que o codigo nao foi encontrado na bio. Oferece opcoes: tentar novamente, verificar instrucoes, ou verificar se a bio esta publica.
- **FE02 - Lodestone fora do ar**: Sistema informa que a verificacao nao pode ser feita no momento e sugere tentar mais tarde.
- **FE03 - Bio privada**: Sistema informa que o perfil do Lodestone pode estar com privacidade ativada e orienta o jogador a tornar a bio publica temporariamente.

---

### UC04 - Login e Recuperacao de Senha

| Campo | Descricao |
|-------|-----------|
| **ID** | UC04 |
| **Nome** | Login e Recuperacao de Senha |
| **Atores** | Jogador (primario), Sistema de Email (secundario) |
| **Pre-condicoes** | Conta existente |
| **Pos-condicoes** | Jogador autenticado com sessao ativa |

**Fluxo Principal:**

1. Jogador acessa a pagina de login
2. Jogador informa email e senha
3. Sistema valida as credenciais
4. Sistema cria sessao autenticada (JWT)
5. Jogador e redirecionado ao dashboard

**Fluxos Alternativos:**

- **FA01 - Login via OAuth**: Jogador clica em "Login com Google" ou "Login com Discord". Sistema autentica via provedor OAuth e cria sessao.
- **FA02 - Recuperacao de senha**: Jogador clica em "Esqueci minha senha". Informa email. Sistema envia link de redefinicao (expira em 1h). Jogador define nova senha.
- **FA03 - Lembrar dispositivo**: Jogador marca "Lembrar-me" para manter sessao ativa por 30 dias.

**Fluxos de Excecao:**

- **FE01 - Credenciais invalidas**: Sistema informa "Email ou senha incorretos" (sem revelar qual esta errado).
- **FE02 - Conta bloqueada**: Apos 5 tentativas falhas, conta e bloqueada por 15 minutos. Sistema informa o bloqueio.
- **FE03 - Conta nao verificada**: Sistema informa que a conta precisa ser verificada e oferece reenvio do email.

---

## Modulo 2: Static

### UC05 - Criar Static

| Campo | Descricao |
|-------|-----------|
| **ID** | UC05 |
| **Nome** | Criar Static |
| **Atores** | Jogador/Lider (primario) |
| **Pre-condicoes** | Conta ativa, personagem verificado. Usuario free: nao possuir outra static como lider. Usuario premium: sem restricao. |
| **Pos-condicoes** | Static criada com o jogador como lider, chat de grupo criado automaticamente |

**Fluxo Principal:**

1. Jogador acessa a pagina de criacao de static
2. Sistema verifica se o jogador pode criar uma nova static (limite free/premium)
3. Jogador preenche informacoes da static:
   - Nome da static
   - Descricao (texto livre)
   - Escopo de conteudo: seleciona tipos de conteudo que a static foca (Savage, Ultimate, Extreme, Criterion, etc.)
   - Composicao desejada: define slots por role/job (ex: 2 Tanks, 2 Healers, 4 DPS)
   - Horarios de raid: dias e horarios regulares
   - Timezone principal
   - Idioma(s) da static
   - Regras de loot (Need/Greed, Round Robin, Loot Council, etc.)
   - Nivel de seriedade (casual, midcore, hardcore)
   - Requisitos minimos (iLvl, experiencia, parse)
4. Jogador seleciona qual personagem sera o lider
5. Sistema cria a static com status "Ativa"
6. Sistema cria automaticamente um chat de grupo para a static
7. Sistema marca o jogador como lider e membro com o job/role selecionado
8. Sistema redireciona para a pagina da static

**Fluxos Alternativos:**

- **FA01 - Static minimal**: Jogador preenche apenas nome e composicao, deixando demais campos com valores padrao.
- **FA02 - Importar membros**: Jogador convida membros existentes na plataforma diretamente durante a criacao (ver UC09).

**Fluxos de Excecao:**

- **FE01 - Limite de statics atingido (free)**: Sistema informa o limite e sugere upgrade para premium.
- **FE02 - Nome ja em uso**: Sistema informa que o nome ja existe e solicita outro.
- **FE03 - Personagem nao verificado**: Sistema solicita verificacao Lodestone antes de permitir criacao.

---

### UC06 - Gerenciar Static

| Campo | Descricao |
|-------|-----------|
| **ID** | UC06 |
| **Nome** | Gerenciar Static |
| **Atores** | Lider de Static (primario) |
| **Pre-condicoes** | Static existente, usuario e o lider |
| **Pos-condicoes** | Configuracoes da static atualizadas |

**Fluxo Principal:**

1. Lider acessa o painel de gestao da static
2. Lider visualiza: membros atuais, vagas, candidaturas pendentes, proximos eventos
3. Lider pode:
   - Editar informacoes da static (nome, descricao, horarios, regras)
   - Alterar escopo de conteudo (adicionar/remover tipos de conteudo)
   - Modificar composicao desejada (roles/jobs)
   - Gerenciar membros (promover, alterar role, remover)
   - Transferir lideranca para outro membro
   - Gerenciar vagas abertas (abrir/fechar)
   - Ver e responder candidaturas (UC08)
   - Enviar convites (UC09)
4. Sistema salva alteracoes e notifica membros afetados

**Fluxos Alternativos:**

- **FA01 - Transferir lideranca**: Lider seleciona um membro e confirma a transferencia. Novo lider recebe notificacao. Antigo lider torna-se membro comum.
- **FA02 - Remover membro**: Lider seleciona membro para remocao, informa motivo (opcional). Membro recebe notificacao e e removido da static e do chat de grupo.
- **FA03 - Alterar role de membro**: Lider altera o job/role designado a um membro na composicao.

**Fluxos de Excecao:**

- **FE01 - Lider tenta remover a si mesmo**: Sistema informa que o lider deve transferir a lideranca antes de sair.
- **FE02 - Ultimo membro**: Se a remocao deixaria a static vazia, sistema sugere dissolver a static (UC07).

---

### UC07 - Dissolver Static

| Campo | Descricao |
|-------|-----------|
| **ID** | UC07 |
| **Nome** | Dissolver Static |
| **Atores** | Lider de Static (primario) |
| **Pre-condicoes** | Static existente, usuario e o lider |
| **Pos-condicoes** | Static marcada como "Dissolvida", membros notificados, historico preservado |

**Fluxo Principal:**

1. Lider acessa configuracoes da static e seleciona "Dissolver Static"
2. Sistema exibe aviso de confirmacao com as consequencias:
   - Todos os membros serao removidos
   - Vagas e candidaturas serao canceladas
   - Eventos futuros serao cancelados
   - Historico e avaliacoes serao preservados
3. Lider confirma digitando o nome da static
4. Sistema marca a static como "Dissolvida"
5. Sistema notifica todos os membros
6. Sistema cancela candidaturas pendentes com motivo "Static dissolvida"
7. Sistema cancela eventos futuros
8. Sistema arquiva o chat de grupo (somente leitura)

**Fluxos Alternativos:**

- **FA01 - Cancelar dissolucao**: Lider desiste antes da confirmacao final.

**Fluxos de Excecao:**

- **FE01 - Nao e o lider**: Sistema bloqueia a acao e informa que apenas o lider pode dissolver a static.

---

### UC08 - Candidatar-se a uma Static

| Campo | Descricao |
|-------|-----------|
| **ID** | UC08 |
| **Nome** | Candidatar-se a uma Static |
| **Atores** | Jogador (primario), Lider de Static (secundario) |
| **Pre-condicoes** | Conta ativa, personagem verificado, static com vaga aberta compativel |
| **Pos-condicoes** | Candidatura registrada com status "Pendente", lider notificado |

**Fluxo Principal:**

1. Jogador encontra uma static (via busca UC10 ou sugestao de match UC12)
2. Jogador visualiza a pagina da static: descricao, membros, vagas, horarios, requisitos
3. Jogador clica em "Candidatar-se" na vaga desejada
4. Sistema verifica compatibilidade basica:
   - Jogador possui o role/job solicitado
   - Horarios nao conflitam com statics existentes (aviso, nao bloqueio)
5. Jogador preenche mensagem de candidatura:
   - Mensagem de apresentacao (texto livre)
   - Job(s) que pretende jogar
   - Disponibilidade confirmada
   - Experiencia relevante
6. Jogador envia a candidatura
7. Sistema registra com status "Pendente"
8. Sistema notifica o lider da static
9. Lider pode: Aceitar, Recusar (com motivo opcional) ou Ignorar

**Fluxos Alternativos:**

- **FA01 - Aceite pelo lider**: Lider aceita a candidatura. Jogador recebe notificacao. Jogador e adicionado a static e ao chat de grupo. Vaga e fechada se preenchida.
- **FA02 - Recusa pelo lider**: Lider recusa a candidatura com motivo opcional. Jogador recebe notificacao com o motivo.
- **FA03 - Cancelamento pelo jogador**: Jogador cancela sua candidatura antes da resposta do lider.

**Fluxos de Excecao:**

- **FE01 - Vaga ja preenchida**: Sistema informa que a vaga foi preenchida durante o processo e sugere outras vagas.
- **FE02 - Ja candidatado**: Sistema informa que o jogador ja tem candidatura ativa nesta static.
- **FE03 - Candidatura expirada**: Apos 14 dias sem resposta, sistema marca como "Expirada" e notifica o jogador.

---

### UC09 - Convidar Jogador para Static

| Campo | Descricao |
|-------|-----------|
| **ID** | UC09 |
| **Nome** | Convidar Jogador para Static |
| **Atores** | Lider de Static (primario), Jogador (secundario) |
| **Pre-condicoes** | Static existente com vaga aberta, jogador alvo com perfil verificado |
| **Pos-condicoes** | Convite enviado ao jogador, jogador notificado |

**Fluxo Principal:**

1. Lider encontra um jogador (via busca, matching ou perfil)
2. Lider visualiza o perfil do jogador: jobs, disponibilidade, progressao, reputacao
3. Lider clica em "Convidar para Static"
4. Lider seleciona para qual vaga/role o convite se destina
5. Lider escreve mensagem de convite (opcional)
6. Sistema envia o convite ao jogador
7. Jogador recebe notificacao do convite
8. Jogador pode: Aceitar, Recusar (com motivo opcional) ou Ignorar

**Fluxos Alternativos:**

- **FA01 - Aceite pelo jogador**: Jogador aceita o convite. Lider recebe notificacao. Jogador e adicionado a static e ao chat de grupo. Vaga e fechada se preenchida.
- **FA02 - Recusa pelo jogador**: Jogador recusa com motivo opcional. Lider recebe notificacao.
- **FA03 - Cancelamento pelo lider**: Lider cancela o convite antes da resposta do jogador.

**Fluxos de Excecao:**

- **FE01 - Jogador ja membro**: Sistema informa que o jogador ja e membro desta static.
- **FE02 - Convite duplicado**: Sistema informa que ja existe um convite pendente para este jogador nesta static.
- **FE03 - Convite expirado**: Apos 7 dias sem resposta, sistema marca como "Expirado" e notifica o lider.
- **FE04 - Jogador bloqueou o lider**: Sistema informa genericamente que o convite nao pode ser enviado.

---

### UC10 - Buscar Statics

| Campo | Descricao |
|-------|-----------|
| **ID** | UC10 |
| **Nome** | Buscar Statics |
| **Atores** | Jogador (primario) |
| **Pre-condicoes** | Conta ativa |
| **Pos-condicoes** | Lista de statics compatíveis exibida |

**Fluxo Principal:**

1. Jogador acessa a pagina de busca de statics
2. Jogador aplica filtros disponiveis:
   - **Data Center / Servidor**
   - **Role/Job necessario**
   - **Tipo de conteudo** (Savage, Ultimate, Extreme, etc.)
   - **Dias e horarios de raid**
   - **Idioma**
   - **Nivel de seriedade** (casual, midcore, hardcore)
   - **Com vagas abertas** (toggle)
3. Sistema executa a busca e retorna resultados ordenados por relevancia
4. Para cada resultado, exibe: nome da static, membros atuais, vagas, horarios, conteudo, reputacao media
5. Jogador pode clicar em uma static para ver detalhes completos
6. Jogador pode se candidatar (UC08) diretamente dos resultados

**Fluxos Alternativos:**

- **FA01 - Filtros avancados (premium)**: Usuario premium pode filtrar por: parse medio dos membros, historico de clears, tempo de existencia da static, taxa de retencao de membros.
- **FA02 - Salvar busca**: Jogador salva os filtros para receber notificacoes quando novas statics compativeis surgirem.
- **FA03 - Busca sem filtros**: Jogador faz busca geral; sistema ordena por statics do mesmo Data Center com vagas abertas.

**Fluxos de Excecao:**

- **FE01 - Nenhum resultado**: Sistema informa que nao encontrou statics com os filtros aplicados e sugere ampliar os criterios.

---

### UC11 - Buscar Jogadores

| Campo | Descricao |
|-------|-----------|
| **ID** | UC11 |
| **Nome** | Buscar Jogadores |
| **Atores** | Lider de Static (primario) |
| **Pre-condicoes** | Conta ativa, static existente com vaga aberta |
| **Pos-condicoes** | Lista de jogadores compativeis exibida |

**Fluxo Principal:**

1. Lider acessa a pagina de busca de jogadores
2. Lider aplica filtros:
   - **Data Center / Servidor**
   - **Role/Job**
   - **Tipo de conteudo**
   - **Disponibilidade horaria**
   - **Idioma**
   - **Status "Buscando Static" (LFS)**
   - **Nivel de progressao**
3. Sistema executa a busca e retorna jogadores ordenados por compatibilidade com a vaga
4. Para cada resultado, exibe: nome do personagem, jobs, disponibilidade, progressao, reputacao
5. Lider pode clicar no perfil para ver detalhes completos
6. Lider pode enviar convite (UC09) diretamente dos resultados

**Fluxos Alternativos:**

- **FA01 - Filtros avancados (premium)**: Lider pode filtrar por: parse minimo, historico de clears, tempo em statics anteriores, reputacao minima.
- **FA02 - Salvar busca**: Lider salva os filtros para receber notificacoes quando novos jogadores compativeis se cadastrarem.

**Fluxos de Excecao:**

- **FE01 - Nenhum resultado**: Sistema sugere ampliar criterios ou aguardar novos cadastros (com notificacao).

---

## Modulo 3: Matching

### UC12 - Receber Sugestoes de Match Automatico

| Campo | Descricao |
|-------|-----------|
| **ID** | UC12 |
| **Nome** | Receber Sugestoes de Match Automatico |
| **Atores** | Jogador ou Lider (primario), Sistema de Matching (secundario) |
| **Pre-condicoes** | Perfil completo e verificado, status LFS ativo (jogador) ou vagas abertas (static) |
| **Pos-condicoes** | Sugestoes de match exibidas com score de compatibilidade |

**Fluxo Principal:**

1. Sistema de Matching executa periodicamente (diariamente) o algoritmo de matching
2. Para cada jogador LFS, o sistema:
   a. Cruza perfil do jogador com vagas abertas de statics
   b. Calcula score de compatibilidade baseado em:
      - Compatibilidade de role/job (peso: 25%)
      - Compatibilidade de horario (peso: 25%)
      - Mesmo Data Center (peso: 15%)
      - Idioma em comum (peso: 10%)
      - Nivel de progressao similar (peso: 10%)
      - Tipo de conteudo alinhado (peso: 10%)
      - Nivel de seriedade compativel (peso: 5%)
   c. Filtra matches abaixo de 40% de compatibilidade
   d. Ordena por score decrescente
3. Para cada static com vagas, o sistema repete o processo para jogadores LFS
4. Sistema notifica jogadores e lideres sobre novas sugestoes
5. Jogador/Lider acessa a secao "Sugestoes de Match" no dashboard
6. Para cada sugestao, visualiza: score total, breakdown por criterio, perfil resumido
7. Jogador/Lider pode: ver perfil completo, candidatar-se/convidar, ou descartar a sugestao

**Fluxos Alternativos:**

- **FA01 - Limite free**: Usuarios free visualizam ate 5 sugestoes por semana (as de maior score). Badge indica que existem mais sugestoes disponiveis com premium.
- **FA02 - Matching prioritario (premium)**: Perfis premium aparecem no topo das sugestoes para lideres e statics.
- **FA03 - Match descartado**: Sugestao descartada nao reaparece. Sistema aprende com descarte para refinar futuras sugestoes.

**Fluxos de Excecao:**

- **FE01 - Perfil incompleto**: Sistema informa quais campos faltam para ativar o matching e direciona para edicao de perfil.
- **FE02 - Nenhum match encontrado**: Sistema informa que nao ha matches compativeis no momento e sugere ampliar preferencias.

---

### UC13 - Configurar Preferencias de Matching

| Campo | Descricao |
|-------|-----------|
| **ID** | UC13 |
| **Nome** | Configurar Preferencias de Matching |
| **Atores** | Jogador ou Lider (primario) |
| **Pre-condicoes** | Conta ativa, perfil existente |
| **Pos-condicoes** | Preferencias de matching atualizadas, futuras sugestoes refletem novos criterios |

**Fluxo Principal:**

1. Jogador/Lider acessa configuracoes de matching no perfil
2. Jogador/Lider ajusta preferencias:
   - **Importancia dos criterios**: ajusta peso relativo de cada criterio (horario, job, progressao, idioma, etc.)
   - **Filtros obrigatorios**: define criterios que DEVEM ser atendidos (ex: "apenas mesmo Data Center", "apenas ingles")
   - **Filtros preferenciais**: define criterios desejados mas nao obrigatorios
   - **Blacklist**: Data Centers, servidores ou tipos de conteudo que nao interessam
3. Sistema salva as preferencias
4. Sistema recalcula sugestoes na proxima execucao do matching

**Fluxos Alternativos:**

- **FA01 - Reset para padrao**: Jogador/Lider restaura preferencias para os valores padrao do sistema.
- **FA02 - Preferencias por static (lider)**: Lider configura preferencias de matching especificas para cada static que lidera.

**Fluxos de Excecao:**

- **FE01 - Filtros muito restritivos**: Sistema alerta que os filtros obrigatorios podem reduzir drasticamente o numero de matches e sugere flexibilizar.

---

### UC14 - Visualizar Detalhes de Compatibilidade

| Campo | Descricao |
|-------|-----------|
| **ID** | UC14 |
| **Nome** | Visualizar Detalhes de Compatibilidade |
| **Atores** | Jogador ou Lider (primario) |
| **Pre-condicoes** | Sugestao de match existente |
| **Pos-condicoes** | Detalhes de compatibilidade visualizados |

**Fluxo Principal:**

1. Jogador/Lider clica em uma sugestao de match
2. Sistema exibe painel detalhado de compatibilidade:
   - **Score total** (ex: 87%)
   - **Breakdown por criterio**:
     - Role/Job: "WHM - vaga de Healer aberta" (100%)
     - Horario: "3 de 4 dias compativeis" (75%)
     - Data Center: "Mesmo DC - Primal" (100%)
     - Idioma: "Ingles em comum" (100%)
     - Progressao: "P11S cleared vs P10S-P12S prog" (80%)
     - Conteudo: "Savage - alinhado" (100%)
     - Seriedade: "Midcore vs Midcore" (100%)
   - **Pontos de atencao**: conflitos ou incompatibilidades parciais destacados
3. Jogador/Lider visualiza o perfil completo do jogador/static sugerido
4. Jogador/Lider decide: candidatar-se/convidar, enviar mensagem, ou descartar

**Fluxos Alternativos:**

- **FA01 - Comparacao lado a lado (premium)**: Premium pode comparar ate 3 sugestoes lado a lado para escolher a melhor.

**Fluxos de Excecao:**

- Nenhum fluxo de excecao especifico.

---

## Modulo 4: Calendario

### UC15 - Criar Evento de Raid

| Campo | Descricao |
|-------|-----------|
| **ID** | UC15 |
| **Nome** | Criar Evento de Raid |
| **Atores** | Lider de Static (primario para eventos de static), Jogador (primario para eventos PUG - premium) |
| **Pre-condicoes** | Para evento de static: ser lider da static. Para PUG: conta premium ativa. |
| **Pos-condicoes** | Evento criado, membros/publico notificados |

**Fluxo Principal:**

1. Lider acessa o calendario da static
2. Lider clica em "Criar Evento"
3. Lider preenche:
   - Titulo do evento (ex: "P12S Prog Night")
   - Descricao / notas
   - Tipo de conteudo (Savage, Ultimate, Extreme, etc.)
   - Encounter especifico (ex: "P12S - The Final Day Phase 2")
   - Data e horario de inicio
   - Data e horario de termino (estimado)
   - Timezone
   - Recorrente? (Sim: seleciona padrao - semanal, bisemanal)
   - Visibilidade: Privado (apenas membros) ou Publico (PUG)
4. Se publico (PUG):
   - Define numero maximo de participantes
   - Define roles necessarios (composicao)
   - Define requisitos minimos (progressao, iLvl, etc.)
5. Sistema cria o evento
6. Sistema notifica membros da static (evento privado) ou publica no calendario publico (PUG)
7. Sistema agenda lembretes automaticos (24h e 1h antes)

**Fluxos Alternativos:**

- **FA01 - Evento recorrente**: Sistema cria multiplas instancias do evento com base no padrao (ex: toda terca, 20h-23h). Cada instancia pode ser editada individualmente.
- **FA02 - Copia de evento**: Lider duplica um evento existente ajustando data/horario.
- **FA03 - Evento multi-dia**: Evento abrange mais de um dia (ex: prog marathon de fim de semana).

**Fluxos de Excecao:**

- **FE01 - Conflito de horario**: Sistema alerta que o horario conflita com outro evento da mesma static. Permite criar mesmo assim.
- **FE02 - PUG sem premium**: Sistema informa que eventos publicos PUG exigem conta premium e sugere upgrade.

---

### UC16 - Confirmar Presenca em Evento

| Campo | Descricao |
|-------|-----------|
| **ID** | UC16 |
| **Nome** | Confirmar Presenca em Evento |
| **Atores** | Membro de Static ou Jogador (primario) |
| **Pre-condicoes** | Evento existente, usuario e membro da static ou evento e PUG publico |
| **Pos-condicoes** | Presenca registrada, lider/organizador informado |

**Fluxo Principal:**

1. Jogador recebe notificacao de novo evento ou acessa o calendario
2. Jogador visualiza detalhes do evento: data, horario, conteudo, membros confirmados
3. Jogador seleciona seu status:
   - **Confirmado**: estara presente
   - **Tentativo**: talvez esteja presente
   - **Ausente**: nao podera comparecer
4. Jogador pode adicionar nota (ex: "posso chegar 15min atrasado")
5. Sistema registra a presenca
6. Se "Ausente": sistema notifica o lider para buscar substituto

**Fluxos Alternativos:**

- **FA01 - Alterar presenca**: Jogador altera seu status a qualquer momento antes do evento.
- **FA02 - Presenca em PUG**: Jogador se inscreve em evento PUG publico. Se vagas esgotadas, entra na lista de espera.
- **FA03 - Presenca automatica**: Se membro de static e evento recorrente, sistema assume "Confirmado" por padrao (configuravel).

**Fluxos de Excecao:**

- **FE01 - Evento lotado (PUG)**: Sistema informa que as vagas estao preenchidas e oferece lista de espera.
- **FE02 - Conflito de horario**: Sistema alerta que o jogador tem outro evento no mesmo horario. Permite confirmar mesmo assim.

---

### UC17 - Gerenciar Eventos PUG Publicos

| Campo | Descricao |
|-------|-----------|
| **ID** | UC17 |
| **Nome** | Gerenciar Eventos PUG Publicos |
| **Atores** | Organizador PUG (primario), Jogadores (secundario) |
| **Pre-condicoes** | Conta premium, evento PUG criado |
| **Pos-condicoes** | Evento PUG gerenciado com participantes confirmados |

**Fluxo Principal:**

1. Organizador acessa o painel do evento PUG
2. Organizador visualiza:
   - Lista de inscritos com perfil resumido (job, progressao, reputacao)
   - Composicao atual vs desejada
   - Lista de espera
3. Organizador pode:
   - Aprovar/recusar inscricoes
   - Definir composicao final
   - Mover jogadores da lista de espera para confirmados
   - Enviar mensagem para todos os inscritos
   - Cancelar o evento (com notificacao para inscritos)
   - Editar detalhes do evento
4. Sistema atualiza os participantes e envia notificacoes

**Fluxos Alternativos:**

- **FA01 - Aceitacao automatica**: Organizador habilita aceitacao automatica para inscricoes que atendam todos os requisitos.
- **FA02 - Substituicao de jogador**: Participante cancela presenca; proximo da lista de espera e notificado automaticamente.

**Fluxos de Excecao:**

- **FE01 - Cancelamento tardio**: Evento cancelado com menos de 2h de antecedencia. Sistema registra o cancelamento no perfil do organizador.

---

### UC18 - Receber Lembretes de Evento

| Campo | Descricao |
|-------|-----------|
| **ID** | UC18 |
| **Nome** | Receber Lembretes de Evento |
| **Atores** | Jogador (primario), Sistema de Notificacoes (secundario) |
| **Pre-condicoes** | Evento existente, jogador com presenca confirmada ou tentativa |
| **Pos-condicoes** | Jogador notificado sobre o evento proximo |

**Fluxo Principal:**

1. Sistema verifica eventos proximos (24h e 1h antes)
2. Para cada evento, identifica jogadores com presenca Confirmada ou Tentativa
3. Sistema envia lembrete com: nome do evento, horario, conteudo, membros confirmados
4. Lembrete enviado via notificacao in-app

**Fluxos Alternativos:**

- **FA01 - Lembretes premium**: Usuarios premium recebem lembretes adicionais via: email, push notification, webhook Discord. Horarios de lembrete sao customizaveis (ex: 48h, 12h, 30min antes).
- **FA02 - Lembrete de ausencia**: Sistema envia lembrete ao lider listando membros que ainda nao responderam presenca.

**Fluxos de Excecao:**

- **FE01 - Notificacao falhou**: Sistema tenta reenviar uma vez. Se falhar novamente, registra log para diagnostico.

---

### UC19 - Exportar Calendario

| Campo | Descricao |
|-------|-----------|
| **ID** | UC19 |
| **Nome** | Exportar Calendario |
| **Atores** | Jogador (primario) |
| **Pre-condicoes** | Conta premium ativa, eventos existentes |
| **Pos-condicoes** | URL de calendario iCal gerada e acessivel |

**Fluxo Principal:**

1. Jogador acessa configuracoes de calendario
2. Jogador clica em "Exportar Calendario"
3. Sistema gera URL unica do feed iCal contendo todos os eventos do jogador (statics + PUGs inscritos)
4. Sistema exibe a URL e instrucoes para importar em:
   - Google Calendar
   - Apple Calendar
   - Outlook
   - Outros apps compativeis com iCal
5. Jogador copia a URL e importa no aplicativo de calendario

**Fluxos Alternativos:**

- **FA01 - Exportar static especifica**: Jogador exporta apenas eventos de uma static especifica.
- **FA02 - Regenerar URL**: Jogador regenera a URL (invalida a anterior) por seguranca.

**Fluxos de Excecao:**

- **FE01 - Sem premium**: Sistema informa que exportacao de calendario e recurso premium.

---

## Modulo 5: Reputacao

### UC20 - Avaliar Jogador ou Static

| Campo | Descricao |
|-------|-----------|
| **ID** | UC20 |
| **Nome** | Avaliar Jogador ou Static |
| **Atores** | Jogador (primario) |
| **Pre-condicoes** | Jogador foi membro da static por pelo menos 2 semanas, ou a static avaliada teve o jogador por pelo menos 2 semanas |
| **Pos-condicoes** | Avaliacao registrada, reputacao do alvo atualizada |

**Fluxo Principal:**

1. Jogador acessa o perfil do jogador ou static que deseja avaliar
2. Sistema verifica que o requisito de 2 semanas de convivencia e atendido
3. Jogador preenche avaliacao:
   - **Pontualidade** (1-5 estrelas): comparecimento nos horarios combinados
   - **Comunicacao** (1-5 estrelas): clareza e frequencia de comunicacao
   - **Performance** (1-5 estrelas): habilidade tecnica e mecanica
   - **Atitude** (1-5 estrelas): comportamento, respeito, positividade
   - **Comprometimento** (1-5 estrelas): dedicacao ao grupo e metas
   - **Comentario** (texto livre, opcional): observacoes adicionais
4. Jogador envia a avaliacao
5. Sistema registra a avaliacao
6. Sistema recalcula a nota de reputacao do alvo (media ponderada com peso maior para avaliacoes recentes)
7. Alvo recebe notificacao de nova avaliacao

**Fluxos Alternativos:**

- **FA01 - Avaliacao de static**: Membro avalia a static como um todo (organizacao, ambiente, comunicacao do lider) usando os mesmos criterios.
- **FA02 - Editar avaliacao**: Jogador edita uma avaliacao feita anteriormente (ate 7 dias apos publicacao).
- **FA03 - Avaliacao apos saida**: Jogador que saiu ou foi removido da static pode avaliar ate 30 dias apos a saida.

**Fluxos de Excecao:**

- **FE01 - Tempo insuficiente**: Sistema informa que o jogador nao atingiu 2 semanas de convivencia e exibe a data em que a avaliacao sera permitida.
- **FE02 - Ja avaliado recentemente**: Sistema informa que ja existe avaliacao recente (limite de 1 avaliacao por par jogador-static a cada 90 dias).
- **FE03 - Auto-avaliacao**: Sistema bloqueia tentativa de avaliar a si mesmo.

---

### UC21 - Visualizar Reputacao

| Campo | Descricao |
|-------|-----------|
| **ID** | UC21 |
| **Nome** | Visualizar Reputacao |
| **Atores** | Jogador (primario) |
| **Pre-condicoes** | Nenhuma (reputacao e publica) |
| **Pos-condicoes** | Informacoes de reputacao exibidas |

**Fluxo Principal:**

1. Jogador acessa o perfil de um jogador ou static
2. Sistema exibe secao de reputacao:
   - **Nota geral** (media 1-5 estrelas)
   - **Numero total de avaliacoes**
   - **Breakdown por criterio**: media individual de pontualidade, comunicacao, performance, atitude, comprometimento
   - **Avaliacoes recentes**: ultimas 5 avaliacoes com comentarios
   - **Tendencia**: indicador se a reputacao esta subindo, estavel ou descendo
3. Jogador pode clicar em "Ver todas as avaliacoes" para lista completa

**Fluxos Alternativos:**

- **FA01 - Historico completo (premium)**: Usuarios premium veem o historico completo de avaliacoes com graficos de evolucao ao longo do tempo.
- **FA02 - Reputacao da static**: Exibe a media das avaliacoes recebidas pela static, incluindo avaliacoes de membros anteriores.
- **FA03 - Sem avaliacoes**: Sistema exibe "Sem avaliacoes ainda" com badge de "Novo na plataforma".

**Fluxos de Excecao:**

- Nenhum fluxo de excecao especifico.

---

### UC22 - Reportar Avaliacao Abusiva

| Campo | Descricao |
|-------|-----------|
| **ID** | UC22 |
| **Nome** | Reportar Avaliacao Abusiva |
| **Atores** | Jogador (primario), Administrador (secundario) |
| **Pre-condicoes** | Avaliacao existente |
| **Pos-condicoes** | Report registrado para analise da moderacao |

**Fluxo Principal:**

1. Jogador visualiza uma avaliacao que considera abusiva
2. Jogador clica em "Reportar"
3. Jogador seleciona o motivo:
   - Linguagem ofensiva ou discriminatoria
   - Informacao falsa ou difamatoria
   - Avaliacao de retaliacao (revenge review)
   - Spam ou conteudo irrelevante
   - Outro (campo livre)
4. Jogador adiciona detalhes (opcional)
5. Sistema registra o report
6. Sistema notifica a equipe de moderacao
7. Moderador analisa o report e decide:
   - Manter a avaliacao (report rejeitado)
   - Remover a avaliacao (report aceito)
   - Suspender o autor da avaliacao (em casos graves)
8. Jogador que reportou e notificado da decisao

**Fluxos Alternativos:**

- **FA01 - Report com evidencia**: Jogador anexa screenshots ou links como evidencia do abuso.
- **FA02 - Multiplos reports**: Se uma avaliacao recebe 3+ reports de diferentes usuarios, e automaticamente ocultada ate moderacao.

**Fluxos de Excecao:**

- **FE01 - Report duplicado**: Sistema informa que o jogador ja reportou esta avaliacao.
- **FE02 - Abuso de reports**: Se um jogador faz reports frequentes que sao rejeitados, sistema alerta sobre uso indevido.

---

## Modulo 6: Chat

### UC23 - Enviar Mensagem Direta

| Campo | Descricao |
|-------|-----------|
| **ID** | UC23 |
| **Nome** | Enviar Mensagem Direta |
| **Atores** | Jogador (primario) |
| **Pre-condicoes** | Conta ativa, destinatario existente e nao bloqueado. Free: menos de 20 conversas ativas. |
| **Pos-condicoes** | Mensagem enviada e entregue ao destinatario |

**Fluxo Principal:**

1. Jogador acessa o perfil de outro jogador ou lider
2. Jogador clica em "Enviar Mensagem"
3. Sistema verifica:
   - Destinatario nao bloqueou o remetente
   - Remetente nao atingiu limite de conversas (free: 20 ativas)
4. Sistema abre/cria sala de chat direta (DM)
5. Jogador digita e envia a mensagem
6. Sistema entrega a mensagem ao destinatario
7. Destinatario recebe notificacao de nova mensagem

**Fluxos Alternativos:**

- **FA01 - Conversa existente**: Se ja existe conversa com o destinatario, sistema abre a conversa existente.
- **FA02 - Enviar imagem/link (premium)**: Usuarios premium podem enviar imagens e links nas mensagens.
- **FA03 - Resposta rapida**: Jogador responde diretamente de uma notificacao sem abrir o app completo.

**Fluxos de Excecao:**

- **FE01 - Usuario bloqueado**: Sistema informa que nao e possivel enviar mensagem (sem revelar que foi bloqueado).
- **FE02 - Limite de conversas (free)**: Sistema informa o limite e sugere encerrar conversas inativas ou fazer upgrade para premium.
- **FE03 - Mensagem muito longa**: Sistema informa limite de caracteres (2000) e solicita reducao.

---

### UC24 - Participar do Chat de Static

| Campo | Descricao |
|-------|-----------|
| **ID** | UC24 |
| **Nome** | Participar do Chat de Static |
| **Atores** | Membro de Static (primario) |
| **Pre-condicoes** | Ser membro ativo da static |
| **Pos-condicoes** | Mensagem enviada e visivel para todos os membros da static |

**Fluxo Principal:**

1. Membro acessa a pagina da static ou a lista de chats
2. Membro abre o chat de grupo da static
3. Sistema exibe historico de mensagens recentes
4. Membro digita e envia mensagem
5. Sistema distribui a mensagem para todos os membros da static em tempo real
6. Membros online veem a mensagem instantaneamente
7. Membros offline recebem notificacao

**Fluxos Alternativos:**

- **FA01 - Mencionar membro**: Membro usa @nome para mencionar alguem especifico, gerando notificacao destacada.
- **FA02 - Fixar mensagem**: Lider pode fixar mensagens importantes (ex: estrategias, regras, links uteis).
- **FA03 - Historico completo (premium)**: Usuarios premium acessam historico completo de mensagens. Free visualiza ultimos 30 dias.

**Fluxos de Excecao:**

- **FE01 - Membro removido**: Se membro for removido da static, perde acesso ao chat imediatamente.
- **FE02 - Chat arquivado**: Se a static foi dissolvida, chat e somente leitura.

---

### UC25 - Gerenciar Notificacoes

| Campo | Descricao |
|-------|-----------|
| **ID** | UC25 |
| **Nome** | Gerenciar Notificacoes |
| **Atores** | Jogador (primario), Sistema de Notificacoes (secundario) |
| **Pre-condicoes** | Conta ativa |
| **Pos-condicoes** | Preferencias de notificacao atualizadas |

**Fluxo Principal:**

1. Jogador acessa configuracoes de notificacao
2. Sistema exibe categorias de notificacao:
   - **Candidaturas e convites**: novas candidaturas, respostas, convites recebidos
   - **Static**: novos membros, saidas, alteracoes de horario, dissolucao
   - **Calendario**: lembretes de evento, alteracoes, cancelamentos
   - **Chat**: novas mensagens diretas, mencoes no chat de static
   - **Matching**: novas sugestoes de match
   - **Reputacao**: novas avaliacoes recebidas
   - **Sistema**: atualizacoes da plataforma, manutencoes
3. Para cada categoria, jogador define:
   - Ativo/Inativo (toggle)
   - Canal: In-app (padrao), Email, Push (premium), Discord webhook (premium)
4. Sistema salva preferencias

**Fluxos Alternativos:**

- **FA01 - Modo nao perturbe**: Jogador ativa periodo de silencio (ex: 23h-8h) onde notificacoes sao acumuladas e entregues depois.
- **FA02 - Mute de conversa**: Jogador silencia notificacoes de uma conversa especifica sem desativar a categoria inteira.
- **FA03 - Email digest (premium)**: Premium configura resumo diario ou semanal por email com todas as notificacoes.

**Fluxos de Excecao:**

- **FE01 - Canal premium**: Sistema informa que canais de push e webhook exigem premium ao tentar ativar.

---

## Modulo 7: Premium

### UC26 - Assinar Plano Premium

| Campo | Descricao |
|-------|-----------|
| **ID** | UC26 |
| **Nome** | Assinar Plano Premium |
| **Atores** | Jogador (primario), Sistema de Pagamentos (secundario) |
| **Pre-condicoes** | Conta ativa, nao possuir assinatura premium ativa |
| **Pos-condicoes** | Assinatura ativa, recursos premium desbloqueados imediatamente |

**Fluxo Principal:**

1. Jogador acessa a pagina de planos premium
2. Sistema exibe opcoes:
   - **Mensal**: valor X/mes, renovacao automatica
   - **Anual**: valor Y/ano (equivalente a Z/mes com desconto de W%), renovacao automatica
3. Jogador seleciona o plano desejado
4. Jogador informa metodo de pagamento (cartao de credito, PayPal, etc.)
5. Sistema processa o pagamento via gateway
6. Pagamento aprovado
7. Sistema ativa a assinatura premium imediatamente
8. Sistema desbloqueia todos os recursos premium
9. Jogador recebe confirmacao por email com detalhes da assinatura

**Fluxos Alternativos:**

- **FA01 - Trial gratuito**: Novo usuario (nunca usou trial) pode ativar 7 dias de premium gratis. Apos o trial, volta para free ou assina.
- **FA02 - Upgrade mensal para anual**: Assinante mensal faz upgrade para anual. Sistema calcula credito proporcional do periodo mensal restante.
- **FA03 - Cupom de desconto**: Jogador aplica codigo promocional antes do pagamento.

**Fluxos de Excecao:**

- **FE01 - Pagamento recusado**: Sistema informa o motivo generico e solicita outro metodo de pagamento.
- **FE02 - Ja possui premium**: Sistema informa que o usuario ja possui assinatura ativa e direciona para gestao da conta.
- **FE03 - Trial ja utilizado**: Sistema informa que o trial ja foi utilizado e oferece assinatura direta.

---

### UC27 - Acessar Recursos Premium

| Campo | Descricao |
|-------|-----------|
| **ID** | UC27 |
| **Nome** | Acessar Recursos Premium |
| **Atores** | Jogador Premium (primario) |
| **Pre-condicoes** | Assinatura premium ativa |
| **Pos-condicoes** | Recurso premium utilizado |

**Fluxo Principal:**

1. Jogador premium navega pela plataforma normalmente
2. Sistema identifica recursos premium e os disponibiliza:
   - **Busca**: filtros avancados habilitados (parse, clear times, historico)
   - **Matching**: sugestoes ilimitadas, matching prioritario
   - **Static**: criacao de statics ilimitadas, pagina personalizada
   - **Calendario**: criacao de eventos PUG, lembretes customizados, exportacao iCal
   - **Chat**: conversas ilimitadas, envio de imagens/links, historico completo
   - **Reputacao**: historico completo, analytics, selo de verificacao
   - **Notificacoes**: push, email, webhooks Discord
   - **Perfil**: badge premium, destaque em buscas, estatisticas avancadas
3. Jogador utiliza o recurso premium desejado

**Fluxos Alternativos:**

- **FA01 - Indicacao de premium**: Quando usuario free encontra recurso premium, sistema exibe teaser com preview do resultado e botao "Desbloqueie com Premium".
- **FA02 - Comparacao lado a lado**: Na pagina de premium, sistema mostra comparativo free vs premium interativo baseado no uso real do jogador.

**Fluxos de Excecao:**

- **FE01 - Assinatura expirada**: Sistema reverte para free e exibe notificacao com opcao de renovacao. Statics extras ficam em modo somente leitura.

---

### UC28 - Gerenciar Assinatura Premium

| Campo | Descricao |
|-------|-----------|
| **ID** | UC28 |
| **Nome** | Gerenciar Assinatura Premium |
| **Atores** | Jogador Premium (primario), Sistema de Pagamentos (secundario) |
| **Pre-condicoes** | Assinatura premium existente (ativa ou expirada) |
| **Pos-condicoes** | Configuracoes de assinatura atualizadas |

**Fluxo Principal:**

1. Jogador acessa a pagina de gestao de assinatura
2. Sistema exibe informacoes da assinatura:
   - Plano atual (mensal/anual)
   - Data de inicio
   - Data de renovacao / expiracao
   - Metodo de pagamento
   - Historico de pagamentos
   - Renovacao automatica (ativa/inativa)
3. Jogador pode:
   - Alterar metodo de pagamento
   - Alterar plano (mensal <-> anual)
   - Ativar/desativar renovacao automatica
   - Cancelar assinatura
   - Baixar recibos/faturas

**Fluxos Alternativos:**

- **FA01 - Cancelamento**: Jogador cancela a assinatura. Sistema exibe pesquisa de saida (motivo do cancelamento). Premium permanece ativo ate o fim do periodo pago. Sistema envia email de confirmacao com data de expiracao.
- **FA02 - Reativacao**: Jogador com assinatura cancelada (mas ainda no periodo pago) pode reativar a renovacao automatica.
- **FA03 - Downgrade gracioso**: Ao expirar premium, sistema mantem statics extras em modo somente leitura (sem deletar). Jogador pode escolher qual static manter ativa.

**Fluxos de Excecao:**

- **FE01 - Falha na renovacao**: Pagamento automatico falha. Sistema envia email alertando e da prazo de 7 dias para regularizar antes de reverter para free.
- **FE02 - Disputa de pagamento**: Sistema suspende premium e direciona para suporte.

---

## Apendice A: Matriz de Rastreabilidade (Requisitos x Use Cases)

| Requisito | Use Cases Relacionados |
|-----------|----------------------|
| Recrutamento bidirecional | UC08, UC09, UC10, UC11 |
| Matching automatico avancado | UC12, UC13, UC14 |
| Calendario com PUG | UC15, UC16, UC17, UC18, UC19 |
| Reputacao publica | UC20, UC21, UC22 |
| Integracao Lodestone | UC02, UC03 |
| Chat interno | UC23, UC24, UC25 |
| Modelo Freemium | UC26, UC27, UC28 |
| Multilingual | UC01 (selecao de idioma), UC02 (idiomas falados), UC12 (matching por idioma) |
| Escopo customizavel | UC05 (criacao), UC06 (gestao), UC15 (eventos) |

## Apendice B: Matriz Atores x Use Cases

| Ator | Use Cases |
|------|-----------|
| **Jogador** | UC01, UC02, UC03, UC04, UC08, UC10, UC12, UC13, UC14, UC16, UC18, UC19, UC20, UC21, UC22, UC23, UC24, UC25, UC26, UC27, UC28 |
| **Lider de Static** | UC05, UC06, UC07, UC09, UC11, UC15, UC17 |
| **Membro de Static** | UC08, UC16, UC24 |
| **Organizador PUG** | UC15 (PUG), UC17 |
| **Administrador** | UC22 |
| **Sistema de Matching** | UC12 |
| **Lodestone/XIVAPI** | UC02, UC03 |
| **Sistema de Notificacoes** | UC18, UC25 |
| **Sistema de Pagamentos** | UC26, UC28 |

## Apendice C: Regras de Negocio x Use Cases

| Regra de Negocio | Use Cases |
|-----------------|-----------|
| RN01-RN05 (Perfil) | UC01, UC02, UC03 |
| RN06-RN12 (Static) | UC05, UC06, UC07 |
| RN13-RN18 (Recrutamento) | UC08, UC09, UC10, UC11 |
| RN19-RN23 (Matching) | UC12, UC13, UC14 |
| RN24-RN29 (Calendario) | UC15, UC16, UC17, UC18, UC19 |
| RN30-RN36 (Reputacao) | UC20, UC21, UC22 |
| RN37-RN41 (Chat) | UC23, UC24, UC25 |
| RN42-RN45 (Premium) | UC26, UC27, UC28 |
