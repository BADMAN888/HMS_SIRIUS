package com.sirius.sirius.store.entity.biling;

import com.sirius.sirius.store.enums.ComponentPriceSource;
import com.sirius.sirius.store.enums.ComponentQuantityBasis;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "service_components", indexes = {
        @Index(name = "idx_service_component_rate", columnList = "rate_id, active, sort_order"),
        @Index(name = "idx_service_component_parent", columnList = "parent_facility_id, active, sort_order")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceComponentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_id")
    RateEntity rate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_facility_id")
    FacilityEntity parentFacility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    FacilityEntity facility;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_source", nullable = false, length = 30)
    ComponentPriceSource priceSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_basis", nullable = false, length = 40)
    ComponentQuantityBasis quantityBasis;

    @Column(nullable = false, precision = 10, scale = 3)
    BigDecimal multiplier;

    @Column(nullable = false)
    boolean included;

    @Column(name = "sort_order", nullable = false)
    int sortOrder;

    @Column(nullable = false)
    boolean active;
}
