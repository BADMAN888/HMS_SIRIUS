package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.FolioStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FolioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    ReservationEntity reservation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    FolioStatus status;

    @Column(nullable = false, precision = 12, scale = 2, name = "total_amount")
    BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2, name = "paid_amount")
    BigDecimal paidAmount;

    @OneToMany(mappedBy = "folio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<AccrualEntity> accruals = new ArrayList<>();

    @OneToMany(mappedBy = "folio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<PaymentEntity> payments = new ArrayList<>();

    public void recalculateTotalAmount() {
        totalAmount = accruals.stream()
                .map(AccrualEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void recalculatePaidAmount() {
        paidAmount = payments.stream()
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getBalance() {
        return totalAmount.subtract(paidAmount);
    }
}