package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.AccrualSource;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accruals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccrualEntity {

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
    @JoinColumn(name = "recipient_id")
    CustomerEntity recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folio_id")
    FolioEntity folio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    FacilityEntity facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    AccrualEntity reversalOf;

    @Column(nullable = false)
    @Builder.Default
    boolean reversed = false;

    @Column(nullable = false, length = 255)
    String name;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 3)
    BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(nullable = false, name = "service_date")
    LocalDate serviceDate;

    @Column(nullable = false, name = "accrued_at")
    LocalDateTime accruedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    @Builder.Default
    AccrualSource source = AccrualSource.MANUAL;

    @PrePersist
    void prePersist() {
        if (accruedAt == null) {
            accruedAt = LocalDateTime.now();
        }
    }
}