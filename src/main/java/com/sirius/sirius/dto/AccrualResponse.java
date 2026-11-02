package com.sirius.sirius.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AccrualResponse(
        Long id,
        Long hotelId,
        Long reservationId,
        Long recipientId,
        Long folioId,
        Long facilityId,
        String name,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal amount,
        LocalDate serviceDate,
        LocalDateTime accruedAt,
        boolean reversed,
        Long reversalOfId
) {
}