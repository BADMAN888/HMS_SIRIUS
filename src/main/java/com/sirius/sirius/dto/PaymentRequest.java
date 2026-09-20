package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull
        Long folioId,

        @NotNull
        PaymentMethod method,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @Size(max = 255)
        String description
) {
}