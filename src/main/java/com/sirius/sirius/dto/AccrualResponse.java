package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.AccrualType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccrualResponse(

        Long id,

        Long folioId,

        AccrualType type,

        String description,

        BigDecimal unitPrice,

        BigDecimal quantity,

        BigDecimal amount,

        LocalDateTime accruedAt
) {
}