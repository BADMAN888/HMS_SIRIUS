package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PackageAccrualRequest(
        @NotNull @Positive Long facilityId,
        @Positive Long reservationId,
        @Positive Long recipientId,
        @Positive Long folioId,
        @NotNull LocalDate serviceDate,
        @NotNull @Positive @Digits(integer = 7, fraction = 3) BigDecimal quantity,
        @Positive Integer guestCount
) {
}
