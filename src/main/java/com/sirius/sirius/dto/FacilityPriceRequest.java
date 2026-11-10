
package com.sirius.sirius.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FacilityPriceRequest(

        @NotNull
        @Positive
        Long facilityId,

        @NotNull
        @Positive
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull
        LocalDate validFrom,

        LocalDate validTo
) {
}
