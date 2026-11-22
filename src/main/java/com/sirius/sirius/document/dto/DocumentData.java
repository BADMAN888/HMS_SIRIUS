package com.sirius.sirius.document.dto;

public record DocumentData(
        String hotelName,
        String hotelAddress,
        String hotelPhone,
        String hotelEmail,
        String primaryGuestFullName,
        String dateOfBirth,
        String phoneNumber,
        String email,
        String nationality,
        String address,
        String reservationNumber,
        String checkInDate,
        String checkInTime,
        String checkOutDate,
        String checkOutTime,
        String roomNumber,
        String roomCategory,
        String rateName,
        String guestCount,
        String reservationStatus
) {}
