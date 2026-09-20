package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.enums.RoomView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequest(

        @NotBlank
        @Size(max = 10)
        String roomNumber,

        @NotNull
        Integer floor,

        @NotNull
        RoomStatus status,

        @NotNull
        RoomView roomView,

        @NotNull
        Long hotelId,

        @NotNull
        Long categoryId
) {
}