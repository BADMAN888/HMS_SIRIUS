package com.sirius.sirius.dto;

import java.math.BigDecimal;
import java.util.List;

public record RateQuoteResponse(
        Long rateId,
        String currency,
        int nights,
        BigDecimal total,
        List<RateQuoteItem> components
) {
}
