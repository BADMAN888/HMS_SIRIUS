package com.sirius.sirius.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import com.sirius.sirius.dto.*;
import com.sirius.sirius.service.billing.CashRegisterService;
import com.sirius.sirius.store.enums.CashOperationType;
import com.sirius.sirius.store.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cash-registers")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class CashRegisterController {
    private final CashRegisterService service;

    @PreAuthorize("@hotelAccess.canAccessHotel(#p0)")
    @GetMapping public List<CashRegisterResponse> list(@RequestParam @Positive Long hotelId) { return service.list(hotelId); }
    @PreAuthorize("@hotelAccess.canAccessCashRegister(#p0)")
    @GetMapping("/{id}") public CashRegisterResponse get(@PathVariable @Positive Long id) { return service.getRegister(id); }
    @PreAuthorize("@hotelAccess.canCreateCashRegister(#p0)")
    @PostMapping public ResponseEntity<CashRegisterResponse> create(@Valid @RequestBody CashRegisterRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @PreAuthorize("@hotelAccess.canAccessCashRegister(#p0)")
    @GetMapping("/{id}/current-shift") public CashShiftResponse currentShift(@PathVariable @Positive Long id) { return service.currentShift(id); }
    @PreAuthorize("@hotelAccess.canAccessCashRegister(#p0)")
    @PostMapping("/{id}/shifts/open") public CashShiftResponse open(@PathVariable @Positive Long id,@Valid @RequestBody CashShiftOpenRequest request) { return service.openShift(id,request); }
    @PreAuthorize("@hotelAccess.canAccessCashRegister(#p0)")
    @PostMapping("/{id}/shifts/{shiftId}/service-in") public CashOperationResponse serviceIn(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId,@Valid @RequestBody CashServiceOperationRequest request) { return service.serviceOperation(id,shiftId,true,request); }
    @PreAuthorize("@hotelAccess.canAccessCashRegister(#p0)")
    @PostMapping("/{id}/shifts/{shiftId}/service-out") public CashOperationResponse serviceOut(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId,@Valid @RequestBody CashServiceOperationRequest request) { return service.serviceOperation(id,shiftId,false,request); }
    @PreAuthorize("@hotelAccess.canAccessCashRegister(#p0)")
    @GetMapping("/{id}/shifts/{shiftId}/operations") public List<CashOperationResponse> operations(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId) { return service.shiftOperations(id,shiftId); }
    @PreAuthorize("@hotelAccess.canAccessCashRegister(#p0)")
    @GetMapping("/{id}/shifts/{shiftId}/x-report") public CashRegisterReportResponse xReport(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId) { return service.xReport(id,shiftId); }
    @PreAuthorize("@hotelAccess.canAccessCashRegister(#p0)")
    @PostMapping("/{id}/shifts/{shiftId}/z-report") public CashRegisterReportResponse zReport(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId,@Valid @RequestBody CashShiftCloseRequest request) { return service.closeShift(id,shiftId,request); }
}
