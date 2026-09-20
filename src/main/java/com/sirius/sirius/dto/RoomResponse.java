package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.enums.RoomView;

public record RoomResponse(

        Long id,

        String roomNumber,

        Integer floor,

        RoomStatus status,

        RoomView roomView,

        Long hotelId,

        Long categoryId
) {
}