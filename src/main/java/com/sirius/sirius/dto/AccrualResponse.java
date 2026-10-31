package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.AccrualType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccrualResponse(

        Long id,

        Long hotelId,

        Long reservationId,

        Long recipientId,

        Long folioId,

        AccrualType type,

        String name,

        BigDecimal unitPrice,

        BigDecimal quantity,

        BigDecimal amount,

        LocalDateTime accruedAt

) {
}