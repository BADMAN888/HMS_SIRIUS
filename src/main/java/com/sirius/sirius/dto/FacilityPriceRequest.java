package com.sirius.sirius.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FacilityPriceRequest(

        @NotNull
        Long facilityId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal price,

        @NotNull
        LocalDate validFrom,

        LocalDate validTo
) {
}
