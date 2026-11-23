package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ComponentPriceSource;
import com.sirius.sirius.store.enums.ComponentQuantityBasis;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ServiceComponentRequest(
        @Positive Long rateId,
        @Positive Long parentFacilityId,
        @Positive Long facilityId,
        ComponentPriceSource priceSource,
        ComponentQuantityBasis quantityBasis,
        @DecimalMin(value = "0.001") @Digits(integer = 7, fraction = 3) BigDecimal multiplier,
        boolean included,
        int sortOrder,
        boolean active
) {
}
