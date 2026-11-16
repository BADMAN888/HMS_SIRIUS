package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record ReservationGuestsRequest(

        Long primaryGuestId,

        Set<@NotNull @Positive Long> guestIds

) {
}
