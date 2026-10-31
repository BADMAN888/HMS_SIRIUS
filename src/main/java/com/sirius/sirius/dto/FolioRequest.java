package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;

public record FolioRequest(

        @NotNull
        Long hotelId,

        Long reservationId,

        Long recipientId,

        Long payerId

) {
}