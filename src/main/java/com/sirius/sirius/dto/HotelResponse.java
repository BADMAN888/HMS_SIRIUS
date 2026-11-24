package com.sirius.sirius.dto;

import java.time.LocalTime;
import java.util.List;

public record HotelResponse(
        Long id,
        String hotelName,
        String description,
        String address,
        String email,
        String phone,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        LocalTime earlyCheckInTime,
        LocalTime lateCheckOutTime,
        List<LegalEntityResponse> legalEntities
) {
}
