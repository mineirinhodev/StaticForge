package com.ffxiv.planner.premium.scheduler;

import com.ffxiv.planner.premium.entity.Subscription;
import com.ffxiv.planner.premium.entity.SubscriptionStatus;
import com.ffxiv.planner.premium.event.SubscriptionExpiredEvent;
import com.ffxiv.planner.premium.event.SubscriptionExpiringSoonEvent;
import com.ffxiv.planner.premium.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Job diário que processa assinaturas expiradas e emite alertas preventivos.
 *
 * <p>Executa todo dia às 01:00 UTC. Usa {@link Clock} injetado para
 * facilitar testes unitários com datas fixas.</p>
 *
 * <p>Regras de negócio:</p>
 * <ul>
 *   <li>ACTIVE/TRIALING com {@code endDate < today} → EXPIRED</li>
 *   <li>CANCELED com {@code endDate < today} → EXPIRED</li>
 *   <li>PAST_DUE com {@code endDate < today - 7 dias} → EXPIRED (período de graça)</li>
 *   <li>ACTIVE com {@code endDate <= today + 3 dias} → alerta preventivo</li>
 * </ul>
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/31
 */
@Component
public class SubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    static final int GRACE_PERIOD_DAYS = 7;
    static final int EXPIRY_ALERT_DAYS = 3;

    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public SubscriptionScheduler(SubscriptionRepository subscriptionRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void processExpiredSubscriptions() {
        LocalDate today = LocalDate.now(clock);
        log.info("Iniciando processamento de assinaturas expiradas para {}", today);

        expireSubscriptions(subscriptionRepository.findExpired(today));
        expireSubscriptions(subscriptionRepository.findCanceledAndExpired(today));
        expireSubscriptions(subscriptionRepository.findPastDueExpired(today.minusDays(GRACE_PERIOD_DAYS)));
        alertExpiringSoon(subscriptionRepository.findExpiringSoon(today.plusDays(EXPIRY_ALERT_DAYS)));

        log.info("Processamento de assinaturas expiradas concluído");
    }

    private void expireSubscriptions(List<Subscription> subscriptions) {
        for (Subscription sub : subscriptions) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            UUID userId = sub.getUser().getId();
            eventPublisher.publishEvent(new SubscriptionExpiredEvent(userId));
            log.debug("Assinatura {} expirada para usuário {}", sub.getId(), userId);
        }
    }

    private void alertExpiringSoon(List<Subscription> subscriptions) {
        for (Subscription sub : subscriptions) {
            UUID userId = sub.getUser().getId();
            eventPublisher.publishEvent(new SubscriptionExpiringSoonEvent(userId, sub.getEndDate()));
            log.debug("Alerta de expiração iminente para usuário {}, endDate={}", userId, sub.getEndDate());
        }
    }
}
