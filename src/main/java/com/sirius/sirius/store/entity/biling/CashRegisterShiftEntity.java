package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.enums.CashShiftStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_register_shifts", indexes = @Index(name = "idx_cash_shift_register_status", columnList = "cash_register_id,status"))
@Getter
@Setter
@NoArgsConstructor
public class CashRegisterShiftEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegisterEntity cashRegister;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opened_by_user_id", nullable = false)
    private UserEntity openedBy;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "opening_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id")
    private UserEntity closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closing_balance_expected", precision = 12, scale = 2)
    private BigDecimal closingBalanceExpected;

    @Column(name = "closing_balance_actual", precision = 12, scale = 2)
    private BigDecimal closingBalanceActual;

    @Column(precision = 12, scale = 2)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashShiftStatus status = CashShiftStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = CashShiftStatus.OPEN;
    }
}
