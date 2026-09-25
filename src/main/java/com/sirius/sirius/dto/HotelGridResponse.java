package com.sirius.sirius.dto;

import java.time.LocalDate;
import java.util.List;

public record HotelGridResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<HotelGridRoomResponse> rooms
) {
}