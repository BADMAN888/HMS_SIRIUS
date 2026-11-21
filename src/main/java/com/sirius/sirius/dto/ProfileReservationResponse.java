package com.sirius.sirius.dto;

import java.time.LocalDateTime;

public record ProfileReservationResponse(
        Long reservationId,
        String confirmationNumber,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        String roomCategory,
        String roomNumber,
        String rateName
) {
}
