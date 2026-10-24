package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;

public record RoomStatusRequest(
        @NotNull RoomStatus status
) {
}