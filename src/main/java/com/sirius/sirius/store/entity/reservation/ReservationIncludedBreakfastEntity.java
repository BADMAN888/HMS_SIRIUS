package com.sirius.sirius.store.entity.reservation;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(
        name = "reservation_included_breakfasts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reservation_included_breakfast_date",
                        columnNames = {"reservation_id", "service_date"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationIncludedBreakfastEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    ReservationEntity reservation;

    @Column(name = "service_date", nullable = false)
    LocalDate serviceDate;

    @Column(nullable = false)
    Integer quantity;

    @Column(name = "served_quantity", nullable = false)
    @Builder.Default
    Integer servedQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    boolean cancelled = false;
}