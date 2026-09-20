package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long folioId,
        PaymentMethod method,
        BigDecimal amount,
        String description,
        LocalDateTime paidAt
) {
}
