package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.RoomUnavailabilityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record RoomUnavailabilityRequest(

        @NotNull
        Long roomId,

        @NotNull
        RoomUnavailabilityStatus status,

        @NotNull
        LocalDateTime startAt,

        @NotNull
        LocalDateTime endAt,

        @NotBlank
        @Size(max = 500)
        String reason
) {
}