package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.CashShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashShiftResponse(
        Long id,
        Long cashRegisterId,
        CashShiftStatus status,
        Long openedByUserId,
        String openedByEmail,
        LocalDateTime openedAt,
        BigDecimal openingBalance,
        BigDecimal cashPayments,
        BigDecimal cashRefunds,
        BigDecimal serviceIn,
        BigDecimal serviceOut,
        BigDecimal currentCash,
        BigDecimal cardPayments,
        BigDecimal bankTransferPayments,
        long operationCount,
        Long closedByUserId,
        String closedByEmail,
        LocalDateTime closedAt,
        BigDecimal closingBalanceExpected,
        BigDecimal closingBalanceActual,
        BigDecimal difference
) {}
