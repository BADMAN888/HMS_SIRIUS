package com.sirius.sirius.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record ReservationRequest(
        @NotNull
        Long roomId,

        @NotNull
        Long rateId,

        @NotNull
        @Valid
        GuestRequest guest,

        @NotNull
        @FutureOrPresent
        LocalDate checkInDate,

        @NotNull
        @Future
        LocalDate checkOutDate,

        @NotNull
        @Positive
        Integer adults,

        @NotNull
        @PositiveOrZero
        Integer children
) {
}