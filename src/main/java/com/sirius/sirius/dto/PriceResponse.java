package com.sirius.sirius.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceResponse(
        Long id,
        Long rateId,
        BigDecimal price,
        LocalDate validFrom,
        LocalDate validTo
) {
}