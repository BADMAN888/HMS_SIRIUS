package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.HousekeepingTaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HousekeepingTaskUpdateRequest(
        @Min(1) int attendantNumber,
        @NotNull HousekeepingTaskStatus status,
        String notes
) {
}
