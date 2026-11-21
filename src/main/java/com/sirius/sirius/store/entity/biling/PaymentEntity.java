
package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.enums.PaymentMethod;
import com.sirius.sirius.store.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folio_id", nullable = false)
    FolioEntity folio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PaymentMethod method;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(length = 255)
    String description;

    @Column(nullable = false, name = "paid_at")
    LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    PaymentEntity reversalOf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "balance_account_id")
    BalanceAccountEntity balanceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    CustomerEntity payer;

    @Column(name = "idempotency_key", length = 64, unique = true)
    String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    UserEntity performedBy;
}
