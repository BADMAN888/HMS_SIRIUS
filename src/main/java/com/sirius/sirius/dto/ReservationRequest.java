
package com.sirius.sirius.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

public record ReservationRequest(
        @NotNull
        Long roomId,

        @NotNull
        Long rateId,

        @NotNull
        @Positive
        Long paymentCategoryId,

        @NotNull
        @Positive
        Long primaryGuestId,

        @NotEmpty
        Set<@NotNull @Positive Long> guestIds,

        @NotNull
        @FutureOrPresent
        LocalDateTime checkIn,

        @NotNull
        @Future
        LocalDateTime checkOut,

        @NotNull
        @Positive
        Integer adults,

        @NotNull
        @PositiveOrZero
        Integer children,

        @Size(max = 100)
        String externalBookingNumber,

        @Size(max = 100)
        String bookingSource,

        @Size(max = 150)
        String company,

        @Size(max = 150)
        String operatorCompany
) {
}
