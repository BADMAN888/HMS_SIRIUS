package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.RoomUnavailabilityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record RoomUnavailabilityBatchRequest(
        @NotNull @Positive Long hotelId,
        @NotEmpty List<@NotNull @Positive Long> roomIds,
        @NotNull RoomUnavailabilityStatus status,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotBlank @Size(max = 500) String reason
) {
}
