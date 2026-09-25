package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationStatus;
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
        Long categoryId,
        List<HotelGridReservationResponse> reservations
) {
}