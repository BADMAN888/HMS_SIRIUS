package com.sirius.sirius.document.dto;

public record DocumentData(
        String primaryGuestFullName,
        String reservationNumber,
        String checkInDate,
        String checkOutDate,
        String roomNumber
) {}
