package com.sirius.sirius.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FacilityPriceResponse(

        Long id,

        Long facilityId,

        String facilityName,

        BigDecimal price,

        LocalDate validFrom,

        LocalDate validTo
) {
}
