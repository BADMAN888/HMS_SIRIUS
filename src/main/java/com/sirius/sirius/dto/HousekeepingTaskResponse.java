package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.HousekeepingTaskStatus;
import com.sirius.sirius.store.enums.HousekeepingTaskType;
import com.sirius.sirius.store.enums.RoomStatus;

import java.time.LocalDateTime;

public record HousekeepingTaskResponse(
        Long id,
        Long roomId,
        String roomNumber,
        Integer floor,
        RoomStatus roomStatus,
        boolean roomClean,
        Long departureReservationId,
        String departureGuest,
        LocalDateTime checkOutAt,
        Long arrivalReservationId,
        String arrivalGuest,
        LocalDateTime checkInAt,
        HousekeepingTaskType taskType,
        HousekeepingTaskStatus status,
        int attendantNumber,
        int priority,
        int estimatedMinutes,
        String notes,
        LocalDateTime completedAt
) {
}
