
package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccrualRequest(
        @NotNull Long facilityId,
        Long reservationId,
        Long recipientId,
        Long folioId,
        @NotNull LocalDate serviceDate,
        @NotNull @Positive BigDecimal quantity
) {
}
