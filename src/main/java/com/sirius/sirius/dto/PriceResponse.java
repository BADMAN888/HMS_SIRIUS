package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.PriceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceResponse(
        Long id,
        Long rateId,
        String rateName,
        Long roomCategoryId,
        String roomCategoryName,
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo,
        PriceStatus status
) {
}