package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ComponentPriceSource;
import com.sirius.sirius.store.enums.ComponentQuantityBasis;

import java.math.BigDecimal;

public record ServiceComponentResponse(
        Long id,
        Long rateId,
        Long parentFacilityId,
        Long facilityId,
        String facilityName,
        ComponentPriceSource priceSource,
        ComponentQuantityBasis quantityBasis,
        BigDecimal multiplier,
        boolean included,
        int sortOrder,
        boolean active
) {
}
