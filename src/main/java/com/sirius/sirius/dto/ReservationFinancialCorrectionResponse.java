package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.ReservationFinancialCorrectionType;

import java.math.BigDecimal;
import java.util.List;

public record ReservationFinancialCorrectionResponse(

        Long folioId,

        Long accrualId,

        Long accrualReversalId,

        List<Long> paymentReversalIds,

        ReservationFinancialCorrectionType type,

        BigDecimal amount,

        Long balanceAccountId

) {
}