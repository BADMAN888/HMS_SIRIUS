
package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.TaxStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

        Long primaryGuestId,

        boolean roomLocked,

        Set<@NotNull @Positive Long> guestIds,

        @NotNull
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
        Long operatorCompanyId,

        @Size(max = 500)
        String guestName,

        @Size(max = 30)
        @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "Вкажіть коректний номер телефону")
        String guestPhone,

        @Size(max = 100)
        @Email(message = "Вкажіть коректну email-адресу")
        String guestEmail,

        boolean earlyCheckInAccepted,
        boolean lateCheckOutAccepted,
        boolean earlyExtraNightAccepted,
        boolean lateExtraNightAccepted

) {
        public ReservationRequest {
                if (taxStatus == null) {
                        taxStatus = TaxStatus.RESIDENT;
                }
        }
}
