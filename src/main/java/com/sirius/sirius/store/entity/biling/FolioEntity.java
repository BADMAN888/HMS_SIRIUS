package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.FolioStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    HotelEntity hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    ReservationEntity reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_profile_id")
    ProfileEntity recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    CustomerEntity payer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    FolioStatus status;

    @Column(nullable = false, precision = 12, scale = 2, name = "total_amount")
    BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2, name = "paid_amount")
    BigDecimal paidAmount;

    @Column(nullable = false, updatable = false, name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "closed_at")
    LocalDateTime closedAt;

    @OneToMany(mappedBy = "folio", cascade = CascadeType.ALL)
    @Builder.Default
    List<AccrualEntity> accruals = new ArrayList<>();

    @OneToMany(mappedBy = "folio", cascade = CascadeType.ALL)
    @Builder.Default
    List<PaymentEntity> payments = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = FolioStatus.OPEN;
        }

        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
    }

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
        return totalAmount.subtract(paidAmount)
                .max(BigDecimal.ZERO);
    }

    public BigDecimal getAdvanceAmount() {
        return paidAmount.subtract(totalAmount)
                .max(BigDecimal.ZERO);
    }
}