package com.sirius.sirius.controller;

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
public class CashRegisterController {
    private final CashRegisterService service;

    @GetMapping public List<CashRegisterResponse> list(@RequestParam @Positive Long hotelId) { return service.list(hotelId); }
    @GetMapping("/{id}") public CashRegisterResponse get(@PathVariable @Positive Long id) { return service.getRegister(id); }
    @PostMapping public ResponseEntity<CashRegisterResponse> create(@Valid @RequestBody CashRegisterRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @GetMapping("/{id}/current-shift") public CashShiftResponse currentShift(@PathVariable @Positive Long id) { return service.currentShift(id); }
    @PostMapping("/{id}/shifts/open") public CashShiftResponse open(@PathVariable @Positive Long id,@Valid @RequestBody CashShiftOpenRequest request) { return service.openShift(id,request); }
    @PostMapping("/{id}/shifts/{shiftId}/service-in") public CashOperationResponse serviceIn(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId,@Valid @RequestBody CashServiceOperationRequest request) { return service.serviceOperation(id,shiftId,true,request); }
    @PostMapping("/{id}/shifts/{shiftId}/service-out") public CashOperationResponse serviceOut(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId,@Valid @RequestBody CashServiceOperationRequest request) { return service.serviceOperation(id,shiftId,false,request); }
    @GetMapping("/{id}/shifts/{shiftId}/operations") public List<CashOperationResponse> operations(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId) { return service.shiftOperations(id,shiftId); }
    @GetMapping("/{id}/shifts/{shiftId}/x-report") public CashRegisterReportResponse xReport(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId) { return service.xReport(id,shiftId); }
    @PostMapping("/{id}/shifts/{shiftId}/z-report") public CashRegisterReportResponse zReport(@PathVariable @Positive Long id,@PathVariable @Positive Long shiftId,@Valid @RequestBody CashShiftCloseRequest request) { return service.closeShift(id,shiftId,request); }
}
