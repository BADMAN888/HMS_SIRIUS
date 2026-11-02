package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.FacilitySystemCode;

public record FacilityResponse(
        Long id,
        Long hotelId,
        String name,
        String description,
        String unit,
        boolean active,
        FacilitySystemCode systemCode
) {}
