package com.sirius.sirius.dto;

import java.time.LocalTime;

public record HotelResponse(
        Long id,
        String hotelName,
        String address,
        String email,
        String phone,
        LocalTime checkInTime,
        LocalTime checkOutTime
) {
}