package com.sirius.sirius.store.entity.biling;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "rate_prices",
        indexes = {
                @Index(
                        name = "idx_rate_price_rate_period",
                        columnList = "rate_id, valid_from, valid_to"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatePriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rate_id", nullable = false)
    RateEntity rate;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal price;

    @Column(nullable = false, name = "valid_from")
    LocalDate validFrom;

    @Column(name = "valid_to")
    LocalDate validTo;
}