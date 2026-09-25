package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationStatus;

import java.time.LocalDateTime;

public record HotelGridReservationResponse(
        Long id,
        String confirmationNumber,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        ReservationStatus status
) {
}