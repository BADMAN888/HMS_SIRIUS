package com.sirius.sirius.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RatePriceResponse(
        Long id,
        Long rateId,
        BigDecimal price,
        LocalDate validFrom,
        LocalDate validTo
) {
}