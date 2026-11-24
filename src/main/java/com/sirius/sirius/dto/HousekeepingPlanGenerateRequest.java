package com.sirius.sirius.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record HousekeepingPlanGenerateRequest(
        @NotNull @Positive Long hotelId,
        @NotNull LocalDate serviceDate,
        @Min(1) @Max(50) int attendantCount
) {
}
