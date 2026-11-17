package com.sirius.sirius.dto;

import jakarta.validation.constraints.NotNull;

public record RoomCleanlinessRequest(
        @NotNull Boolean clean
) {
}
