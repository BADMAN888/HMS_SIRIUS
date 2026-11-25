package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record GroupBookingRoomRequest(
        @NotNull @Positive Long roomId,
        @NotNull @Positive Long rateId,
        @NotNull @Positive Long paymentCategoryId,
        @Positive Long primaryGuestId,
        Set<@NotNull @Positive Long> guestIds,
        @NotNull @Positive Integer adults,
        @NotNull @PositiveOrZero Integer children,
        boolean roomLocked,
        @Size(max = 500) String guestName
) {}
