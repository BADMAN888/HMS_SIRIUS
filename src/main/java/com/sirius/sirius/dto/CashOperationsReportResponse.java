package com.sirius.sirius.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CashOperationsReportResponse(
        LocalDateTime from,
        LocalDateTime to,
        List<CashOperationResponse> operations,
        List<CashLegalEntityTotalsResponse> totalsByLegalEntity
) {}
