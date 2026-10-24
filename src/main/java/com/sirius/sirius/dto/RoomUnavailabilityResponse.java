package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.RoomUnavailabilityStatus;

import java.time.LocalDateTime;

public record RoomUnavailabilityResponse(

        Long id,

        Long roomId,

        String roomNumber,

        RoomUnavailabilityStatus status,

        LocalDateTime startAt,

        LocalDateTime endAt,

        String reason,

        LocalDateTime createdAt,

        LocalDateTime completedAt
) {
}