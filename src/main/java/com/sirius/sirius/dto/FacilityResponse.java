package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.FacilityType;
import com.sirius.sirius.store.enums.TaxStatus;

public record FacilityResponse(
        Long id,
        Long hotelId,
        String name,
        String description,
        String unit,
        boolean active,
        FacilityType type,
        TaxStatus taxStatus
) {
}