package com.ffxiv.planner.premium.entity;

import com.ffxiv.planner.auth.entity.User;
import com.ffxiv.planner.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Assinatura premium de um usuário.
 *
 * <p>Relação 1:1 com {@link User}. O histórico de pagamentos é mantido
 * em {@link PaymentHistory}.</p>
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Subscription extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

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

    // Apenas navegabilidade — PaymentHistory é escrito exclusivamente pelo webhook handler.
    // Sem cascade nem orphanRemoval: registros de pagamento são auditoria imutável.
    @OneToMany(mappedBy = "subscription")
    @OrderBy("paidAt DESC")
    private List<PaymentHistory> payments = new ArrayList<>();
}
