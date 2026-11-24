package com.sirius.sirius.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HousekeepingPlanResponse(
        Long id,
        Long hotelId,
        LocalDate serviceDate,
        int attendantCount,
        LocalDateTime updatedAt,
        List<HousekeepingTaskResponse> tasks
) {
}
