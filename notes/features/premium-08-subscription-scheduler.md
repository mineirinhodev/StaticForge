# PREMIUM-08: SubscriptionScheduler (job diário)

Issue: https://github.com/mineirinhodev/StaticForge/issues/31
TDD Issue: https://github.com/mineirinhodev/StaticForge/issues/129

## Escopo
Job `@Scheduled` diário que processa assinaturas vencidas:

1. **Expira ACTIVE/TRIALING vencidas** — `findExpired(today)` → status EXPIRED + publica `SubscriptionExpiredEvent`
2. **Expira CANCELED vencidas** — `findCanceledAndExpired(today)` → status EXPIRED + publica `SubscriptionExpiredEvent`
3. **Expira PAST_DUE fora da graça** — `findPastDueExpired(today - 7 days)` → status EXPIRED + publica `SubscriptionExpiredEvent`
4. **Alerta expirando em breve** — `findExpiringSoon(today + 3 days)` → publica `SubscriptionExpiredEvent` (notificação preventiva)

## Pacote
`com.ffxiv.planner.premium.scheduler`

## Decisões técnicas
- Usa `@Scheduled(cron = "0 0 1 * * *")` (01:00 UTC todo dia) via Spring Scheduling
- Injeta `Clock` para testabilidade (testes usam `Clock.fixed(...)`)
- `ApplicationEventPublisher` para publicar eventos de domínio
- Lógica de expiração delegada ao `SubscriptionService.expireSubscription(id)` já existente
- `@Transactional` em cada método para garantir atomicidade por lote

## Observações
- `@EnableScheduling` já pode estar no contexto — verificar se precisa adicionar
- Período de graça PAST_DUE: 7 dias (constante no scheduler)
- Janela de alerta "expirando em breve": 3 dias
