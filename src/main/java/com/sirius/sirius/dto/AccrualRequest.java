package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.AccrualType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccrualRequest(

        @NotNull
        Long folioId,
        @NotNull
        AccrualType type,
        @NotBlank
        String description,

        @NotNull @DecimalMin(value = "0.00")
        BigDecimal unitPrice,

        @NotNull @Positive
        BigDecimal quantity) {
}