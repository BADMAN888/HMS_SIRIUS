package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.enums.ReservationCancellationReason;
import com.sirius.sirius.store.enums.TaxStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record ReservationResponse(
        Long id,
        String confirmationNumber,
        RoomResponse room,
        RateResponse rate,
        Long paymentCategoryId,
        String paymentCategoryName,
        ProfileResponse primaryGuest,
        String guestName,
        String guestPhone,
        String guestEmail,
        boolean roomLocked,
        Set<Long> guestIds,
        LocalDateTime checkIn,
        LocalDateTime actualCheckIn,
        LocalDateTime checkOut,
        Integer adults,
        Integer children,
        TaxStatus taxStatus,
        ReservationStatus status,
        ReservationCancellationReason cancellationReason,
        List<ReservationCommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        Long folioId,
        String externalBookingNumber,
        String bookingSource,
        CompanyResponse sourceCompany,
        CompanyResponse operatorCompany
) {
}
