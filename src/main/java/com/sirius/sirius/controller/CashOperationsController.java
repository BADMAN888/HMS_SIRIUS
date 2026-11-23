package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.CashOperationsReportResponse;
import com.sirius.sirius.service.billing.CashRegisterService;
import com.sirius.sirius.store.enums.CashOperationType;
import com.sirius.sirius.store.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CashOperationsController {
    private final CashRegisterService service;

    @PreAuthorize("@hotelAccess.canSearchCashOperations(#p0, #p2)")
    @GetMapping("/api/cash-operations")
    public List<com.sirius.sirius.dto.CashOperationResponse> operations(
            @RequestParam(required=false) Long hotelId,
            @RequestParam(required=false) Long legalEntityId,
            @RequestParam(required=false) Long registerId,
            @RequestParam(required=false) Long cashierId,
            @RequestParam(required=false) CashOperationType type,
            @RequestParam(required=false) PaymentMethod method,
            @RequestParam(required=false) Long shiftId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.search(hotelId,legalEntityId,registerId,cashierId,type,method,shiftId,from,to).operations();
    }

    @PreAuthorize("@hotelAccess.canSearchCashOperations(#p0, #p2)")
    @GetMapping("/api/cash-reports/operations")
    public CashOperationsReportResponse report(
            @RequestParam(required=false) Long hotelId,
            @RequestParam(required=false) Long legalEntityId,
            @RequestParam(required=false) Long registerId,
            @RequestParam(required=false) Long cashierId,
            @RequestParam(required=false) CashOperationType type,
            @RequestParam(required=false) PaymentMethod method,
            @RequestParam(required=false) Long shiftId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.search(hotelId,legalEntityId,registerId,cashierId,type,method,shiftId,from,to);
    }
}
