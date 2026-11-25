package com.sirius.sirius.dto;

import java.time.LocalDateTime;

public record ReservationHistoryResponse(
        Long id,
        Long reservationId,
        String userName,
        String action,
        String field,
        String oldValue,
        String newValue,
        LocalDateTime changedAt
) {}
