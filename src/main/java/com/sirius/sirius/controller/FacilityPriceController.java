package com.sirius.sirius.controller;

import com.sirius.sirius.dto.FacilityPriceRequest;
import com.sirius.sirius.dto.FacilityPriceResponse;
import com.sirius.sirius.service.billing.FacilityPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/facility-prices")
@RequiredArgsConstructor
public class FacilityPriceController {

    private final FacilityPriceService facilityPriceService;

    @PostMapping
    public ResponseEntity<FacilityPriceResponse> create(
            @Valid @RequestBody FacilityPriceRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(facilityPriceService.create(request));
    }

    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<List<FacilityPriceResponse>> getHistory(
            @PathVariable Long facilityId
    ) {
        return ResponseEntity.ok(
                facilityPriceService.getHistory(facilityId)
        );
    }

    @GetMapping("/facility/{facilityId}/date/{date}")
    public ResponseEntity<FacilityPriceResponse> getPriceForDate(
            @PathVariable Long facilityId,
            @PathVariable LocalDate date
    ) {
        return ResponseEntity.ok(
                facilityPriceService.getPriceForDate(facilityId, date)
        );
    }
}
