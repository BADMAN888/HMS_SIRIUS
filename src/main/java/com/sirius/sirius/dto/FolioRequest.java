package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.FolioStatus;
import jakarta.validation.constraints.NotNull;

public record FolioRequest(
        @NotNull
        Long reservationId,

        @NotNull
        FolioStatus status
) {
}
