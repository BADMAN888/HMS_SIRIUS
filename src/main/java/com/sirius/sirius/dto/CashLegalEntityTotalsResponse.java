package com.sirius.sirius.dto;

import java.math.BigDecimal;

public record CashLegalEntityTotalsResponse(
        Long legalEntityId,
        String legalEntityName,
        String legalEntityShortName,
        String edrpou,
        BigDecimal receipts,
        BigDecimal outflows,
        BigDecimal netMovement,
        BigDecimal cash,
        BigDecimal card,
        BigDecimal bankTransfer,
        BigDecimal serviceIn,
        BigDecimal serviceOut,
        BigDecimal refunds
) {}
