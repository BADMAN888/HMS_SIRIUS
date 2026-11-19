package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FolioPartiesRequest(
        @NotNull @Positive Long recipientId,
        @NotNull @Positive Long payerId,
        @NotNull @Positive Long reservationId,
        @Positive Long serviceProviderId
) {
}
