package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.FolioStatus;

import java.math.BigDecimal;
import java.util.List;

public record FolioResponse(
        Long id,
        Long reservationId,
        FolioStatus status,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        List<AccrualResponse> accruals,
        List<PaymentResponse> payments
) {
}
