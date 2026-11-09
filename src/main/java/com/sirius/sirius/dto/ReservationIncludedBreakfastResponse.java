package com.sirius.sirius.dto;

import java.time.LocalDate;

public record ReservationIncludedBreakfastResponse(
        Long id,
        Long reservationId,
        LocalDate serviceDate,
        Integer quantity,
        Integer servedQuantity,
        Integer remainingQuantity,
        boolean cancelled
) {
}