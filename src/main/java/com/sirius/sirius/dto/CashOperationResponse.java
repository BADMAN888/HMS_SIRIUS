package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.CashOperationType;
import com.sirius.sirius.store.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashOperationResponse(
        Long id,
        Long cashRegisterId,
        String cashRegisterName,
        Long shiftId,
        Long legalEntityId,
        String legalEntityName,
        String legalEntityShortName,
        String edrpou,
        CashOperationType operationType,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String description,
        LocalDateTime operationDate,
        Long paymentId,
        Long folioId,
        Long reservationId,
        String reservationNumber,
        Long performedByUserId,
        String performedByEmail,
        Long reversalOfId,
        LocalDateTime createdAt
) {}
