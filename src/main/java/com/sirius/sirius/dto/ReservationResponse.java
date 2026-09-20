package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record ReservationResponse(
        Long id,
        String confirmationNumber,
        Long roomId,
        Long rateId,
        Set<Long> guestIds,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer adults,
        Integer children,
        ReservationStatus status,
        List<ReservationCommentResponse> comments,
        LocalDateTime createdAt,
        Long folioId
) {
}