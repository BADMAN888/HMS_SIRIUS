package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationCancellationReason;
import jakarta.validation.constraints.NotNull;

public record ReservationCancelRequest(
        @NotNull ReservationCancellationReason reason
) {
}
