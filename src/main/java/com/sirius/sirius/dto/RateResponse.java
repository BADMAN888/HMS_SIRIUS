package com.sirius.sirius.dto;

import java.math.BigDecimal;

public record RateResponse(
        Long id,
        String name,
        String description,
        String currency,
        Long categoryId,
        BigDecimal price,
        boolean active
) {
}