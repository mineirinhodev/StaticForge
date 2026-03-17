package com.ffxiv.planner.premium.scheduler;

import com.ffxiv.planner.premium.entity.Subscription;
import com.ffxiv.planner.premium.entity.SubscriptionPlan;
import com.ffxiv.planner.premium.entity.SubscriptionStatus;
import com.ffxiv.planner.premium.event.SubscriptionExpiredEvent;
import com.ffxiv.planner.premium.event.SubscriptionExpiringSoonEvent;
import com.ffxiv.planner.premium.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do SubscriptionScheduler com Clock mockado.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/31
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionSchedulerTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    private Clock clock;
    private SubscriptionScheduler scheduler;

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 16);

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2026-03-16T01:00:00Z"), ZoneOffset.UTC);
        scheduler = new SubscriptionScheduler(subscriptionRepository, eventPublisher, clock);
    }

    // ── expira ACTIVE/TRIALING vencidas ───────────────────────

    @Test
    void processExpired_expiresSingleActivePastDueSubscription() {
        Subscription sub = buildSub(SubscriptionStatus.ACTIVE, TODAY.minusDays(1));
        when(subscriptionRepository.findExpired(TODAY)).thenReturn(List.of(sub));
        when(subscriptionRepository.findCanceledAndExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findPastDueExpired(TODAY.minusDays(7))).thenReturn(List.of());
        when(subscriptionRepository.findExpiringSoon(TODAY.plusDays(3))).thenReturn(List.of());

        scheduler.processExpiredSubscriptions();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(eventPublisher).publishEvent(any(SubscriptionExpiredEvent.class));
    }

    @Test
    void processExpired_expiresMultipleSubscriptions() {
        Subscription sub1 = buildSub(SubscriptionStatus.ACTIVE, TODAY.minusDays(2));
        Subscription sub2 = buildSub(SubscriptionStatus.TRIALING, TODAY.minusDays(1));
        when(subscriptionRepository.findExpired(TODAY)).thenReturn(List.of(sub1, sub2));
        when(subscriptionRepository.findCanceledAndExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findPastDueExpired(TODAY.minusDays(7))).thenReturn(List.of());
        when(subscriptionRepository.findExpiringSoon(TODAY.plusDays(3))).thenReturn(List.of());

        scheduler.processExpiredSubscriptions();

        assertThat(sub1.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(sub2.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(eventPublisher, times(2)).publishEvent(any(SubscriptionExpiredEvent.class));
    }

    // ── expira CANCELED vencidas ──────────────────────────────

    @Test
    void processExpired_expiresCanceledSubscriptionsAfterEndDate() {
        Subscription sub = buildSub(SubscriptionStatus.CANCELED, TODAY.minusDays(1));
        when(subscriptionRepository.findExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findCanceledAndExpired(TODAY)).thenReturn(List.of(sub));
        when(subscriptionRepository.findPastDueExpired(TODAY.minusDays(7))).thenReturn(List.of());
        when(subscriptionRepository.findExpiringSoon(TODAY.plusDays(3))).thenReturn(List.of());

        scheduler.processExpiredSubscriptions();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(eventPublisher).publishEvent(any(SubscriptionExpiredEvent.class));
    }

    // ── expira PAST_DUE fora da graça ─────────────────────────

    @Test
    void processExpired_expiresPastDueAfterGracePeriod() {
        Subscription sub = buildSub(SubscriptionStatus.PAST_DUE, TODAY.minusDays(8));
        when(subscriptionRepository.findExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findCanceledAndExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findPastDueExpired(TODAY.minusDays(7))).thenReturn(List.of(sub));
        when(subscriptionRepository.findExpiringSoon(TODAY.plusDays(3))).thenReturn(List.of());

        scheduler.processExpiredSubscriptions();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(eventPublisher).publishEvent(any(SubscriptionExpiredEvent.class));
    }

    // ── alerta expirando em breve ─────────────────────────────

    @Test
    void processExpired_publishesExpiringSoonEventForUpcomingExpirations() {
        Subscription sub = buildSub(SubscriptionStatus.ACTIVE, TODAY.plusDays(2));
        when(subscriptionRepository.findExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findCanceledAndExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findPastDueExpired(TODAY.minusDays(7))).thenReturn(List.of());
        when(subscriptionRepository.findExpiringSoon(TODAY.plusDays(3))).thenReturn(List.of(sub));

        scheduler.processExpiredSubscriptions();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE); // não expira ainda
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(SubscriptionExpiringSoonEvent.class);
    }

    // ── nada a fazer ──────────────────────────────────────────

    @Test
    void processExpired_doesNothingWhenNoSubscriptionsFound() {
        when(subscriptionRepository.findExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findCanceledAndExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findPastDueExpired(TODAY.minusDays(7))).thenReturn(List.of());
        when(subscriptionRepository.findExpiringSoon(TODAY.plusDays(3))).thenReturn(List.of());

        scheduler.processExpiredSubscriptions();

        verifyNoInteractions(eventPublisher);
    }

    // ── correto uso de datas via Clock ────────────────────────

    @Test
    void processExpired_usesCorrectDatesFromClock() {
        when(subscriptionRepository.findExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findCanceledAndExpired(TODAY)).thenReturn(List.of());
        when(subscriptionRepository.findPastDueExpired(TODAY.minusDays(7))).thenReturn(List.of());
        when(subscriptionRepository.findExpiringSoon(TODAY.plusDays(3))).thenReturn(List.of());

        scheduler.processExpiredSubscriptions();

        verify(subscriptionRepository).findExpired(TODAY);
        verify(subscriptionRepository).findCanceledAndExpired(TODAY);
        verify(subscriptionRepository).findPastDueExpired(TODAY.minusDays(7));
        verify(subscriptionRepository).findExpiringSoon(TODAY.plusDays(3));
    }

    // ── helper ────────────────────────────────────────────────

    private Subscription buildSub(SubscriptionStatus status, LocalDate endDate) {
        Subscription sub = new Subscription();
        sub.setPlan(SubscriptionPlan.MONTHLY);
        sub.setStatus(status);
        sub.setStartDate(endDate.minusDays(30));
        sub.setEndDate(endDate);
        com.ffxiv.planner.auth.entity.User user = new com.ffxiv.planner.auth.entity.User();
        user.setEmail("test@test.com");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        sub.setUser(user);
        return sub;
    }
}
