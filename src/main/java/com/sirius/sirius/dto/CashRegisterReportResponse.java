package com.sirius.sirius.dto;

import com.sirius.sirius.store.enums.CashReportType;

import java.time.LocalDateTime;
import java.util.List;

public record CashRegisterReportResponse(
        Long id,
        CashReportType reportType,
        LocalDateTime generatedAt,
        String snapshot,
        CashShiftResponse shift,
        CashRegisterResponse cashRegister,
        List<CashOperationResponse> operations
) {}
