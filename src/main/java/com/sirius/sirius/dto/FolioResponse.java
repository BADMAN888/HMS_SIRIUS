package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.FolioStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FolioResponse(

        Long id,

        Long hotelId,

        Long reservationId,

        Long recipientId,

        Long payerId,

        FolioStatus status,

        BigDecimal totalAmount,

        BigDecimal paidAmount,

        BigDecimal balance,

        BigDecimal advanceAmount,

        LocalDateTime createdAt,

        LocalDateTime closedAt,

        List<AccrualResponse> accruals,

        List<PaymentResponse> payments

) {
}