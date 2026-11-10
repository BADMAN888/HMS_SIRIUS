
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.TaxStatus;
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
        @Positive
        Long roomId,

        @NotNull
        @Positive
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

        TaxStatus taxStatus,

        @Size(max = 100)
        String externalBookingNumber,

        @Size(max = 100)
        String bookingSource,

        @Positive
        Long sourceCompanyId,

        @Positive
        Long operatorCompanyId

) {
        public ReservationRequest {
                if (taxStatus == null) {
                        taxStatus = TaxStatus.RESIDENT;
                }
        }
}
