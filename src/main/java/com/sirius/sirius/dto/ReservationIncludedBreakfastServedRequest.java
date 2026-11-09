package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReservationIncludedBreakfastServedRequest(

        @NotNull
        @PositiveOrZero
        Integer servedQuantity
) {
}