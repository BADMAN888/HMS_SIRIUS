package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationStatus;

import java.time.LocalDate;

public record HotelGridReservationResponse(
        Long id,
        String confirmationNumber,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        ReservationStatus status
) {
}