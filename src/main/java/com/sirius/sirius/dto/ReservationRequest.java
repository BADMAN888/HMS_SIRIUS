package com.sirius.sirius.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.Set;

public record ReservationRequest(
        @NotNull
        Long roomId,

        @NotNull
        Long rateId,

        @NotNull
        @Positive
        Long primaryGuestId,

        @NotEmpty
        Set<@NotNull @Positive Long> guestIds,

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