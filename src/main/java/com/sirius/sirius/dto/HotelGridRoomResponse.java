package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.enums.RoomView;

import java.util.List;

public record HotelGridRoomResponse(
        Long id,
        String roomNumber,
        Integer floor,
        RoomStatus status,
        RoomView roomView,
        Long hotelId,
        String hotelName,
        Long categoryId,
        String categoryName,
        List<HotelGridReservationResponse> reservations
) {
}