package com.sirius.sirius.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RateQuoteItem(
        Long facilityId,
        String name,
        LocalDate serviceDate,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal amount,
        boolean included,
        Long componentId
) {
}
