
package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FolioRequest(
        @NotNull @Positive Long hotelId,
        @Positive Long reservationId,
        @NotNull @Positive Long recipientId,
        @NotNull @Positive Long payerId
) {
}
