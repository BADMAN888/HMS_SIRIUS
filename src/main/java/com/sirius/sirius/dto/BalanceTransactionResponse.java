package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.BalanceTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceTransactionResponse(

        Long id,

        Long balanceAccountId,

        BalanceTransactionType type,

        BigDecimal amount,

        BigDecimal balanceAfter,

        String description,

        Long reservationId,

        Long folioId,

        LocalDateTime createdAt

) {
}