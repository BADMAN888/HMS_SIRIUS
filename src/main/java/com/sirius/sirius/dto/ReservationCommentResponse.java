package com.sirius.sirius.dto;

import java.time.LocalDateTime;

public record ReservationCommentResponse(
        Long id,
        Long reservationId,
        String text,
        LocalDateTime createdAt
) {
}
