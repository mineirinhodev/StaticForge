package com.ffxiv.planner.premium.entity;

import com.ffxiv.planner.auth.DataJpaTestBase;
import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.common.auditing.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa persistência das entidades Subscription e PaymentHistory
 * contra PostgreSQL real via Testcontainers.
 *
 * Issue: https://github.com/mineirinhodev/StaticForge/issues/25
 */
@Testcontainers
class SubscriptionEntityTest extends DataJpaTestBase {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private TestEntityManager em;

    // ── Subscription ──────────────────────────────────────────

    @Test
    void subscriptionCanBePersistedWithDefaults() {
        User user = em.persist(minimalUser("sub@test.com"));
        Subscription saved = em.persistFlushFind(minimalSubscription(user));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getPlan()).isEqualTo(SubscriptionPlan.MONTHLY);
        assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(saved.isCancelAtPeriodEnd()).isFalse();
        assertThat(saved.isTrialUsed()).isFalse();
        assertThat(saved.isAutoRenew()).isTrue();
    }

    @Test
    void subscriptionAuditFieldsArePopulatedOnPersist() {
        User user = em.persist(minimalUser("sub-audit@test.com"));
        Subscription saved = em.persistFlushFind(minimalSubscription(user));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void subscriptionPlanEnumIsPersistedAsString() {
        User user = em.persist(minimalUser("plan@test.com"));
        Subscription sub = minimalSubscription(user);
        sub.setPlan(SubscriptionPlan.ANNUAL);
        Subscription saved = em.persistFlushFind(sub);

        assertThat(saved.getPlan()).isEqualTo(SubscriptionPlan.ANNUAL);
    }

    @Test
    void subscriptionStatusEnumIsPersistedAsString() {
        User user = em.persist(minimalUser("status@test.com"));
        Subscription sub = minimalSubscription(user);
        sub.setStatus(SubscriptionStatus.TRIALING);
        Subscription saved = em.persistFlushFind(sub);

        assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.TRIALING);
    }

    @Test
    void subscriptionWithStripeIdsCanBePersisted() {
        User user = em.persist(minimalUser("stripe@test.com"));
        Subscription sub = minimalSubscription(user);
        sub.setStripeCustomerId("cus_test123");
        sub.setStripeSubscriptionId("sub_test456");
        Subscription saved = em.persistFlushFind(sub);

        assertThat(saved.getStripeCustomerId()).isEqualTo("cus_test123");
        assertThat(saved.getStripeSubscriptionId()).isEqualTo("sub_test456");
    }

    @Test
    void subscriptionStripeFieldsAreNullByDefault() {
        User user = em.persist(minimalUser("nostripe@test.com"));
        Subscription saved = em.persistFlushFind(minimalSubscription(user));

        assertThat(saved.getStripeCustomerId()).isNull();
        assertThat(saved.getStripeSubscriptionId()).isNull();
    }

    // ── PaymentHistory ────────────────────────────────────────

    @Test
    void paymentHistoryCanBePersistedWithSubscription() {
        User user = em.persist(minimalUser("pay@test.com"));
        Subscription sub = em.persist(minimalSubscription(user));
        PaymentHistory saved = em.persistFlushFind(minimalPayment(sub));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSubscription().getId()).isEqualTo(sub.getId());
        assertThat(saved.getAmount()).isEqualByComparingTo("4.99");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void paymentHistoryStatusEnumIsPersistedAsString() {
        User user = em.persist(minimalUser("paystatus@test.com"));
        Subscription sub = em.persist(minimalSubscription(user));

        PaymentHistory failed = minimalPayment(sub);
        failed.setStatus(PaymentStatus.FAILED);
        failed.setFailureReason("Insufficient funds");
        PaymentHistory saved = em.persistFlushFind(failed);

        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void paymentHistoryWithStripeIdsCanBePersisted() {
        User user = em.persist(minimalUser("paystripe@test.com"));
        Subscription sub = em.persist(minimalSubscription(user));
        PaymentHistory payment = minimalPayment(sub);
        payment.setStripePaymentId("pi_test123");
        payment.setStripeInvoiceId("in_test456");
        PaymentHistory saved = em.persistFlushFind(payment);

        assertThat(saved.getStripePaymentId()).isEqualTo("pi_test123");
        assertThat(saved.getStripeInvoiceId()).isEqualTo("in_test456");
    }

    // ── Relationship ──────────────────────────────────────────

    @Test
    void subscriptionHasPaymentsCollection() {
        User user = em.persist(minimalUser("rel@test.com"));
        Subscription sub = em.persist(minimalSubscription(user));
        em.persist(minimalPayment(sub));
        em.persist(minimalPayment(sub));
        em.flush();
        em.clear();

        Subscription found = em.find(Subscription.class, sub.getId());

        assertThat(found.getPayments()).hasSize(2);
    }

    // ── Helpers ───────────────────────────────────────────────

    private Subscription minimalSubscription(User user) {
        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setPlan(SubscriptionPlan.MONTHLY);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusMonths(1));
        return sub;
    }

    private PaymentHistory minimalPayment(Subscription sub) {
        PaymentHistory payment = new PaymentHistory();
        payment.setSubscription(sub);
        payment.setAmount(new BigDecimal("4.99"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setCreatedAt(Instant.now());
        return payment;
    }
}
