package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.PriceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceRequest(

        @NotNull
        Long rateId,

        @NotNull
        Long roomCategoryId,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal amount,

        @NotNull
        LocalDate validFrom,

        LocalDate validTo,

        @NotNull
        PriceStatus status
) {
}