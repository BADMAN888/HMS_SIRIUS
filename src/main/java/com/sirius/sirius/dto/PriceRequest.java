package com.sirius.sirius.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceRequest(

        @NotNull
        Long rateId,

        @NotNull
        @Positive
        @DecimalMin("0.01")
        BigDecimal price,

        @NotNull
        LocalDate validFrom,

        LocalDate validTo
) {
}