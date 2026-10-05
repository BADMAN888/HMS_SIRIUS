package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReservationCheckInResponse(
        Long id,
        String confirmationNumber,
        Long roomId,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        ReservationStatus status,
        List<ReservationCommentResponse> comments,
        BigDecimal totalAmount
) {
}